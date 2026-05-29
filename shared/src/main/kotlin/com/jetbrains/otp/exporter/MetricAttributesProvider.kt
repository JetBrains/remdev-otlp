package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.jetbrains.otp.span.CommonSpanAttributesState
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

interface MetricAttributesProvider {
    /**
     * Attributes calculated once per process and attached to every exported metric.
     */
    fun getStaticAttributes(): Attributes = Attributes.empty()

    /**
     * Attributes that may change at runtime. Values are cached per provider cache key and TTL.
     */
    fun getDynamicAttributeProviders(): List<DynamicMetricAttributesProvider> = emptyList()

    companion object {
        val EP_NAME = ExtensionPointName.create<MetricAttributesProvider>(
            "com.jetbrains.otp.diagnostic.metricAttributesProvider"
        )
    }
}

interface DynamicMetricAttributesProvider {
    /**
     * Stable cache key for this attribute group.
     */
    fun getCacheKey(): String

    /**
     * Cache TTL in seconds. Values less than or equal to zero disable caching.
     */
    fun getCacheTtlSeconds(): Long

    fun getAttributes(): Attributes
}

internal open class MetricAttributesResolver(
    configuredAttributes: Map<String, String>,
) {
    private val configuredAttributes = toAttributes(configuredAttributes)
    private val attributesCache = ExpiringAttributesCache()
    private val staticAttributesLock = Any()

    @Volatile
    private var staticAttributes: Attributes? = null

    fun getAttributes(): Attributes {
        val builder = Attributes.builder()
        builder.putAll(configuredAttributes)
        builder.putAll(getStaticAttributes())
        builder.putAll(getDynamicAttributes())
        builder.putAll(getRuntimeAttributes())
        return builder.build()
    }

    protected open fun getRuntimeAttributes(): Attributes {
        return CommonSpanAttributesState.snapshot()
    }

    protected open fun getMetricAttributesProviders(): List<MetricAttributesProvider> {
        return MetricAttributesProvider.EP_NAME.extensionList
    }

    private fun getStaticAttributes(): Attributes {
        staticAttributes?.let { return it }

        return synchronized(staticAttributesLock) {
            staticAttributes ?: collectStaticAttributes().also { staticAttributes = it }
        }
    }

    private fun collectStaticAttributes(): Attributes {
        val builder = Attributes.builder()
        for (provider in getMetricAttributesProviders()) {
            val attributes = readSafely(provider, "collect static metric attributes", Attributes.empty()) {
                getStaticAttributes()
            }
            builder.putAll(attributes)
        }
        return builder.build()
    }

    private fun getDynamicAttributes(): Attributes {
        val builder = Attributes.builder()
        for (provider in getMetricAttributesProviders()) {
            val dynamicProviders = readSafely(provider, "collect dynamic metric attribute providers", emptyList()) {
                getDynamicAttributeProviders()
            }
            for (dynamicProvider in dynamicProviders) {
                builder.putAll(resolveDynamicAttributes(dynamicProvider))
            }
        }
        return builder.build()
    }

    private fun resolveDynamicAttributes(provider: DynamicMetricAttributesProvider): Attributes {
        return readSafely(provider, "resolve dynamic metric attributes", Attributes.empty()) {
            val cacheKey = getCacheKey().trim()
            when {
                cacheKey.isEmpty() -> Attributes.empty()
                else -> attributesCache.getOrCompute(providerCacheKey(this, cacheKey), getCacheTtlSeconds()) {
                    getAttributes()
                }
            }
        }
    }

    private fun providerCacheKey(provider: DynamicMetricAttributesProvider, cacheKey: String): String {
        return "${provider.javaClass.name}:$cacheKey"
    }

    private fun <T : Any, R> readSafely(
        source: T,
        action: String,
        defaultValue: R,
        block: T.() -> R,
    ): R {
        return try {
            source.block()
        } catch (error: Throwable) {
            LOG.warn("Failed to $action from ${source.javaClass.name}", error)
            defaultValue
        }
    }

    private companion object {
        private val LOG = Logger.getInstance(MetricAttributesResolver::class.java)

        private fun toAttributes(source: Map<String, String>): Attributes {
            if (source.isEmpty()) return Attributes.empty()

            val builder = Attributes.builder()
            source.forEach { (key, value) ->
                builder.put(AttributeKey.stringKey(key), value)
            }
            return builder.build()
        }
    }
}
