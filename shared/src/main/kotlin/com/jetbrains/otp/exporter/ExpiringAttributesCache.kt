package com.jetbrains.otp.exporter

import io.opentelemetry.api.common.Attributes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal open class ExpiringAttributesCache {
    private val lock = Any()
    private val cachedAttributes = ConcurrentHashMap<String, CachedAttributes>()

    fun getOrCompute(
        key: String,
        ttlSeconds: Long,
        attributesProvider: () -> Attributes,
    ): Attributes {
        val ttlNanos = TimeUnit.SECONDS.toNanos(ttlSeconds.coerceAtLeast(0))
        if (ttlNanos == 0L) return attributesProvider()

        getFreshAttributes(key)?.let { return it }

        return synchronized(lock) {
            getFreshAttributes(key) ?: computeAndCache(key, ttlNanos, attributesProvider)
        }
    }

    protected open fun currentNanoTime(): Long {
        return System.nanoTime()
    }

    private fun getFreshAttributes(key: String): Attributes? {
        val cached = cachedAttributes[key] ?: return null
        if (cached.expiresAtNanos <= currentNanoTime()) return null
        return cached.attributes
    }

    private fun computeAndCache(
        key: String,
        ttlNanos: Long,
        attributesProvider: () -> Attributes,
    ): Attributes {
        val attributes = attributesProvider()
        cachedAttributes[key] = CachedAttributes(
            attributes = attributes,
            expiresAtNanos = currentNanoTime() + ttlNanos,
        )
        return attributes
    }

    private data class CachedAttributes(
        val attributes: Attributes,
        val expiresAtNanos: Long,
    )
}
