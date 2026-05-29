package com.jetbrains.otp.exporter

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExpiringAttributesCacheTest {
    @Test
    fun `reuses value before ttl expires`() {
        val cache = TestExpiringAttributesCache()
        var calls = 0

        assertEquals("value-1", cache.getOrCompute("key", 10) { attributes(++calls) }.value())

        cache.currentTimeNanos = TimeUnit.SECONDS.toNanos(9)

        assertEquals("value-1", cache.getOrCompute("key", 10) { attributes(++calls) }.value())
        assertEquals(1, calls)
    }

    @Test
    fun `refreshes value after ttl expires`() {
        val cache = TestExpiringAttributesCache()
        var calls = 0

        assertEquals("value-1", cache.getOrCompute("key", 10) { attributes(++calls) }.value())

        cache.currentTimeNanos = TimeUnit.SECONDS.toNanos(10)

        assertEquals("value-2", cache.getOrCompute("key", 10) { attributes(++calls) }.value())
        assertEquals(2, calls)
    }

    @Test
    fun `does not cache non-positive ttl`() {
        val cache = TestExpiringAttributesCache()
        var calls = 0

        assertEquals("value-1", cache.getOrCompute("key", 0) { attributes(++calls) }.value())
        assertEquals("value-2", cache.getOrCompute("key", 0) { attributes(++calls) }.value())
        assertEquals(2, calls)
    }

    private fun attributes(value: Int): Attributes {
        return Attributes.of(AttributeKey.stringKey("cached.attribute"), "value-$value")
    }

    private fun Attributes.value(): String? {
        return get(AttributeKey.stringKey("cached.attribute"))
    }

    private class TestExpiringAttributesCache : ExpiringAttributesCache() {
        var currentTimeNanos: Long = 0L

        override fun currentNanoTime(): Long = currentTimeNanos
    }
}
