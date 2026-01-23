package com.jetbrains.otp.exporter.processor

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.exporter.TelemetrySpanExporter
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.SpanData


object SessionProcessor : SpanProcessor {
    private val bufferLock = Any()
    private val bufferedSpans = mutableListOf<SpanData>()
    private val eventsLock = Any()
    private val collectedEvents = mutableListOf<EventData>()

    @Volatile
    private var sessionInitialized = false

    @Volatile
    private var sessionSpanId: String? = null

    @Volatile
    private var sessionTraceId: String? = null

    override fun process(spans: Collection<SpanData>): Collection<SpanData> {
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
            sessionSpanId = spanId
            sessionTraceId = traceId
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
        TelemetrySpanExporter.getInstance().sendSpans(spans)
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
        val spanId = sessionSpanId
        val traceId = sessionTraceId

        if (spanId == null || traceId == null) {
            return spans
        }

        val sessionSpanContext = SpanContext.create(
            traceId,
            spanId,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        )

        return spans.map { span ->
            if (!span.parentSpanContext.isValid && span.name != "remote-dev-session") {
                SpanDelegatingData(span, newParent = sessionSpanContext)
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