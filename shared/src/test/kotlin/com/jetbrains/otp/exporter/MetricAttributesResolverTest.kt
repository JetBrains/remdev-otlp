package com.jetbrains.otp.exporter

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricAttributesResolverTest {
    @Test
    fun `merges configured static dynamic and runtime attributes`() {
        val dynamicProvider = TestDynamicMetricAttributesProvider(
            key = "cache-key",
            ttlSeconds = 60,
        )
        val metricAttributesProvider = TestMetricAttributesProvider(
            staticAttributes = attributes("static.attribute" to "static"),
            dynamicAttributesProviders = listOf(dynamicProvider),
        )
        val resolver = TestMetricAttributesResolver(
            mapOf(
                "configured.attribute" to "configured",
                "runtime.attribute" to "configured-value",
            )
        ).apply {
            runtimeAttrs = attributes("runtime.attribute" to "runtime")
            providers = listOf(metricAttributesProvider)
        }

        val first = resolver.getAttributes()
        val second = resolver.getAttributes()

        assertEquals("configured", first.value("configured.attribute"))
        assertEquals("static", first.value("static.attribute"))
        assertEquals("dynamic-1", first.value("dynamic.attribute"))
        assertEquals("runtime", first.value("runtime.attribute"))
        assertEquals("dynamic-1", second.value("dynamic.attribute"))
        assertEquals(1, metricAttributesProvider.staticAttributeCalls)
        assertEquals(1, dynamicProvider.attributeCalls)
    }

    @Test
    fun `uses cache key changes as cache misses`() {
        val dynamicProvider = TestDynamicMetricAttributesProvider(
            key = "cache-key-1",
            ttlSeconds = 60,
        )
        val resolver = TestMetricAttributesResolver().apply {
            providers = listOf(TestMetricAttributesProvider(dynamicAttributesProviders = listOf(dynamicProvider)))
        }

        assertEquals("dynamic-1", resolver.getAttributes().value("dynamic.attribute"))
        dynamicProvider.key = "cache-key-2"
        assertEquals("dynamic-2", resolver.getAttributes().value("dynamic.attribute"))
        assertEquals(2, dynamicProvider.attributeCalls)
    }

    private fun attributes(vararg values: Pair<String, String>): Attributes {
        val builder = Attributes.builder()
        values.forEach { (key, value) ->
            builder.put(AttributeKey.stringKey(key), value)
        }
        return builder.build()
    }

    private fun Attributes.value(key: String): String? {
        return get(AttributeKey.stringKey(key))
    }

    private class TestMetricAttributesResolver(
        configuredAttributes: Map<String, String> = emptyMap(),
    ) : MetricAttributesResolver(configuredAttributes) {
        var runtimeAttrs: Attributes = Attributes.empty()
        var providers: List<MetricAttributesProvider> = emptyList()

        override fun getRuntimeAttributes(): Attributes = runtimeAttrs

        override fun getMetricAttributesProviders(): List<MetricAttributesProvider> = providers
    }

    private class TestMetricAttributesProvider(
        private val staticAttributes: Attributes = Attributes.empty(),
        private val dynamicAttributesProviders: List<DynamicMetricAttributesProvider> = emptyList(),
    ) : MetricAttributesProvider {
        var staticAttributeCalls = 0

        override fun getStaticAttributes(): Attributes {
            staticAttributeCalls++
            return staticAttributes
        }

        override fun getDynamicAttributeProviders(): List<DynamicMetricAttributesProvider> {
            return dynamicAttributesProviders
        }
    }

    private class TestDynamicMetricAttributesProvider(
        var key: String,
        private val ttlSeconds: Long,
    ) : DynamicMetricAttributesProvider {
        var attributeCalls = 0

        override fun getCacheKey(): String = key

        override fun getCacheTtlSeconds(): Long = ttlSeconds

        override fun getAttributes(): Attributes {
            attributeCalls++
            return Attributes.of(AttributeKey.stringKey("dynamic.attribute"), "dynamic-$attributeCalls")
        }
    }
}
