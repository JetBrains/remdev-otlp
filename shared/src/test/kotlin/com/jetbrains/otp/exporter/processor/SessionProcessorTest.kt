@file:Suppress("DEPRECATION")

package com.jetbrains.otp.exporter.processor

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SessionProcessorTest {
    @Test
    fun `reparents root spans to the session trace`() {
        val sessionContext = spanContext(
            traceId = "11111111111111111111111111111111",
            spanId = "2222222222222222"
        )
        val originalSpan = TestSpanData(
            name = "connection-dropped-reconnecting",
            spanContext = spanContext(
                traceId = "33333333333333333333333333333333",
                spanId = "4444444444444444"
            ),
            parentSpanContext = SpanContext.getInvalid()
        )

        val reparentedSpan = SessionProcessor.reparentToSession(originalSpan, sessionContext)

        assertEquals(sessionContext.traceId, reparentedSpan.traceId)
        assertEquals(sessionContext.traceId, reparentedSpan.spanContext.traceId)
        assertEquals(originalSpan.spanId, reparentedSpan.spanId)
        assertEquals(originalSpan.spanId, reparentedSpan.spanContext.spanId)
        assertEquals(sessionContext.spanId, reparentedSpan.parentSpanId)
        assertEquals(sessionContext.traceId, reparentedSpan.parentSpanContext.traceId)
    }

    @Test
    fun `keeps remote session root span unchanged`() {
        val sessionContext = spanContext(
            traceId = "11111111111111111111111111111111",
            spanId = "2222222222222222"
        )
        val sessionSpan = TestSpanData(
            name = "remote-dev-session",
            spanContext = sessionContext,
            parentSpanContext = SpanContext.getInvalid()
        )

        val result = SessionProcessor.reparentToSession(sessionSpan, sessionContext)

        assertSame(sessionSpan, result)
    }

    @Test
    fun `keeps spans already attached to the session unchanged`() {
        val sessionContext = spanContext(
            traceId = "11111111111111111111111111111111",
            spanId = "2222222222222222"
        )
        val sessionChild = TestSpanData(
            name = "session-metadata",
            spanContext = spanContext(
                traceId = sessionContext.traceId,
                spanId = "6666666666666666"
            ),
            parentSpanContext = sessionContext
        )

        val result = SessionProcessor.reparentToSession(sessionChild, sessionContext)

        assertSame(sessionChild, result)
    }

    @Test
    fun `keeps spans with an existing parent unchanged`() {
        val sessionContext = spanContext(
            traceId = "11111111111111111111111111111111",
            spanId = "2222222222222222"
        )
        val parentContext = spanContext(
            traceId = "33333333333333333333333333333333",
            spanId = "5555555555555555"
        )
        val nestedSpan = TestSpanData(
            name = "ide-exception",
            spanContext = spanContext(
                traceId = parentContext.traceId,
                spanId = "4444444444444444"
            ),
            parentSpanContext = parentContext
        )

        val result = SessionProcessor.reparentToSession(nestedSpan, sessionContext)

        assertSame(nestedSpan, result)
    }

    @Test
    fun `reparents spans whose parent is a filtered application init root`() {
        val sessionContext = spanContext(
            traceId = "11111111111111111111111111111111",
            spanId = "2222222222222222"
        )
        val applicationInitContext = spanContext(
            traceId = "33333333333333333333333333333333",
            spanId = "5555555555555555"
        )
        val exceptionSpan = TestSpanData(
            name = "ide-exception",
            spanContext = spanContext(
                traceId = applicationInitContext.traceId,
                spanId = "4444444444444444"
            ),
            parentSpanContext = applicationInitContext
        )

        val reparentedSpan = SessionProcessor.reparentToSession(
            exceptionSpan,
            sessionContext,
            filteredRootContextKeys = setOf(spanContextKey(applicationInitContext))
        )

        assertEquals(sessionContext.traceId, reparentedSpan.traceId)
        assertEquals(sessionContext.traceId, reparentedSpan.spanContext.traceId)
        assertEquals(exceptionSpan.spanId, reparentedSpan.spanId)
        assertEquals(sessionContext.spanId, reparentedSpan.parentSpanId)
    }

    private fun spanContext(traceId: String, spanId: String): SpanContext {
        return SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault())
    }

    private fun spanContextKey(context: SpanContext): String = "${context.traceId}:${context.spanId}"

    private data class TestSpanData(
        private val name: String,
        private val spanContext: SpanContext,
        private val parentSpanContext: SpanContext,
    ) : SpanData {
        override fun getName(): String = name

        override fun getKind(): SpanKind = SpanKind.INTERNAL

        override fun getSpanContext(): SpanContext = spanContext

        override fun getParentSpanContext(): SpanContext = parentSpanContext

        override fun getStatus(): StatusData = StatusData.unset()

        override fun getStartEpochNanos(): Long = 0L

        override fun getAttributes(): Attributes = Attributes.empty()

        override fun getEvents(): List<EventData> = emptyList()

        override fun getLinks(): List<LinkData> = emptyList()

        override fun getEndEpochNanos(): Long = 1L

        override fun hasEnded(): Boolean = true

        override fun getTotalRecordedEvents(): Int = 0

        override fun getTotalRecordedLinks(): Int = 0

        override fun getTotalAttributeCount(): Int = 0

        @Deprecated("SpanData still requires this deprecated OpenTelemetry API")
        override fun getInstrumentationLibraryInfo(): InstrumentationLibraryInfo = InstrumentationLibraryInfo.empty()

        override fun getResource(): Resource = Resource.empty()
    }
}
