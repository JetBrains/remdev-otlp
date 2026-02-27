package com.jetbrains.otp.exporter.processor

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.exporter.OtlpConfig
import com.jetbrains.otp.exporter.TelemetrySpanExporter
import com.jetbrains.otp.span.DefaultRootSpanService
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.SpanData
import java.time.Instant


object SessionProcessor : SpanProcessor {
    private val bufferLock = Any()
    private val bufferedSpans = mutableListOf<SpanData>()
    private val eventsLock = Any()
    private val collectedEvents = mutableListOf<EventData>()

    @Volatile
    private var sessionInitialized = false

    @Volatile
    private var sessionSpanContext: SpanContext? = null

    override fun process(spans: Collection<SpanData>, config: OtlpConfig): Collection<SpanData> {
        val bufferedEvents = bufferIfNeeded(spans)
        if (bufferedEvents.isEmpty()) {
            return emptyList()
        }

        val spansAfterEventCollection = collectEventsFromApplicationInit(bufferedEvents)
        val spansAfterParentAttachment = attachSessionParent(spansAfterEventCollection)
        return attachBufferedEvents(spansAfterParentAttachment)
    }

    override fun getOrder(): Int = 0

    fun onSessionInitialized(spanId: String, traceId: String) {
        val spans = synchronized(bufferLock) {
            if (sessionInitialized) {
                LOG.debug("Session already initialized, ignoring duplicate call")
                return
            }
            sessionSpanContext = SpanContext.create(
                traceId,
                spanId,
                TraceFlags.getSampled(),
                TraceState.getDefault()
            )
            sessionInitialized = true
            val spans = bufferedSpans.toList()
            bufferedSpans.clear()

            if (spans.isNotEmpty()) {
                LOG.info("Session initialized. Releasing ${spans.size} buffered spans")
            } else {
                LOG.debug("Session initialized. No buffered spans to release")
            }
            spans
        }

        val earliestStartTime = spans.minOfOrNull { it.startEpochNanos } ?: System.nanoTime()
        createMetaSpan(earliestStartTime)
        TelemetrySpanExporter.getInstance().sendSpans(spans)
    }

    private fun createMetaSpan(startTimeNanos: Long) {
        val context = sessionSpanContext ?: return

        val tracer = DefaultRootSpanService.TRACER
        val metaSpan = tracer.spanBuilder("session-metadata")
            .setParent(Context.root().with(Span.wrap(context)))
            .setStartTimestamp(startTimeNanos, java.util.concurrent.TimeUnit.NANOSECONDS)
            .startSpan()

        metaSpan.setAttribute("session.trace_id", context.traceId)
        metaSpan.setAttribute("session.span_id", context.spanId)

        val endTime = Instant.ofEpochSecond(0, startTimeNanos).plusSeconds(2)
        metaSpan.end(endTime)
    }

    private fun bufferIfNeeded(spans: Collection<SpanData>): Collection<SpanData> {
        return synchronized(bufferLock) {
            if (sessionInitialized) {
                spans
            } else {
                bufferedSpans.addAll(spans)
                LOG.debug("Session not initialized. Buffered ${spans.size} spans (total buffered: ${bufferedSpans.size})")
                emptyList()
            }
        }
    }

    private fun collectEventsFromApplicationInit(spans: Collection<SpanData>): Collection<SpanData> {
        val filteredSpans = mutableListOf<SpanData>()

        for (span in spans) {
            if (span.name == "application-init") {
                synchronized(eventsLock) {
                    collectedEvents.addAll(span.events)
                }
                LOG.debug("Captured ${span.events.size} events from application-init span, filtering it out from export")
            } else {
                filteredSpans.add(span)
            }
        }

        return filteredSpans
    }

    private fun attachSessionParent(spans: Collection<SpanData>): Collection<SpanData> {
        val context = sessionSpanContext
        if (context == null || !context.isValid) {
            return spans
        }

        return spans.map { span ->
            if (!span.parentSpanContext.isValid && span.name != "remote-dev-session") {
                SpanDelegatingData(span, newParent = context)
            } else {
                span
            }
        }
    }

    private fun attachBufferedEvents(spans: Collection<SpanData>): Collection<SpanData> {
        val eventsToAttach = extractCollectedEvents()
        if (eventsToAttach.isEmpty()) {
            return spans
        }

        return spans.map { span ->
            if (span.name == "remote-dev-session") {
                LOG.debug("Adding ${eventsToAttach.size} events to session span")
                SpanDelegatingData(span, additionalEvents = eventsToAttach)
            } else {
                span
            }
        }
    }

    private fun extractCollectedEvents(): List<EventData> {
        if (collectedEvents.isEmpty()) return emptyList()

        return synchronized(eventsLock) {
            if (collectedEvents.isEmpty()) return emptyList()
            val events = collectedEvents.toList()
            collectedEvents.clear()
            events
        }
    }

    private val LOG = Logger.getInstance(SessionProcessor::class.java)
}
