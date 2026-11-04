package com.jetbrains.otp.exporter

import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.trace.data.SpanData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class EarlyExportedSpansTest {
    private val exportedEarly = EarlyExportedSpans()

    @Test
    fun `keeps every span while nothing was exported early`() {
        val spans = listOf(span("00f067aa0ba902b7"), span("00f067aa0ba902b8"))

        assertSame(spans, exportedEarly.withoutExported(spans))
    }

    @Test
    fun `drops the platform copy of a span exported early`() {
        val sessionSpan = span("00f067aa0ba902b7")
        val freezeSpan = span("00f067aa0ba902b8")
        exportedEarly.markExported(sessionSpan)

        val pending = exportedEarly.withoutExported(listOf(span("00f067aa0ba902b7"), freezeSpan))

        assertEquals(listOf(freezeSpan), pending)
    }

    @Test
    fun `exports a span early only once`() {
        val sessionSpan = span("00f067aa0ba902b7")

        assertTrue(exportedEarly.markExported(sessionSpan))
        assertFalse(exportedEarly.markExported(sessionSpan))
    }

    private fun span(spanId: String): SpanData {
        val context = SpanContext.create("4bf92f3577b34da6a3ce929d0e0e4736", spanId, TraceFlags.getSampled(), TraceState.getDefault())
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SpanData::class.java)) { proxy, method, args ->
            when (method.name) {
                "getSpanContext" -> context
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "SpanData($spanId)"
                else -> null
            }
        } as SpanData
    }
}
