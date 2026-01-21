package com.jetbrains.otp.exporter

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.settings.SpanFilterService
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.common.InstrumentationScopeInfo
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
class TelemetrySpanExporter {
    private val spanExporter: SpanExporter? by lazy { OtlpSpanExporterFactory.create() }

    private val bufferLock = Any()
    private val bufferedSpans = mutableListOf<SpanData>()

    private val eventsLock = Any()
    private val defaultSpanEvents = mutableListOf<EventData>()

    @Volatile
    private var sessionSpanId: String? = null

    @Volatile
    private var sessionTraceId: String? = null

    fun sendSpans(spanData: Collection<SpanData>) {
        exportSpans(spanData)
    }

    fun sessionSpanInitialized(spanId: String, traceId: String) {
        val spansToFlush = synchronized(bufferLock) {
            if (sessionSpanId != null) {
                LOG.debug("Session already initialized, ignoring duplicate call")
                return
            }
            sessionSpanId = spanId
            sessionTraceId = traceId
            val spans = bufferedSpans.toList()
            bufferedSpans.clear()
            spans
        }

        if (spansToFlush.isNotEmpty()) {
            LOG.info("Session initialized. Flushing ${spansToFlush.size} buffered spans")
            doExport(spansToFlush)
        } else {
            LOG.debug("Session initialized. No buffered spans to flush")
        }
    }

    private fun exportSpans(spanData: Collection<SpanData>) {
        val filteredSpans = filterSpans(spanData)
        if (filteredSpans.isEmpty()) return

        val spansToExport = synchronized(bufferLock) {
            bufferedSpans.addAll(filteredSpans)
            if (sessionSpanId != null) {
                val spans = bufferedSpans.toList()
                bufferedSpans.clear()
                return@synchronized spans
            } else {
                LOG.debug("Session not initialized. Buffered ${filteredSpans.size} spans (total buffered: ${bufferedSpans.size})")
                return
            }
        }

        if (spansToExport.isNotEmpty()) {
            doExport(spansToExport)
        }
    }

    private fun doExport(spans: Collection<SpanData>) {
        val exporter = spanExporter
        if (exporter == null) {
            LOG.debug("Honeycomb exporter not initialized. Spans will not be sent.")
            return
        }

        val processedSpans = processSpans(spans)

        try {
            val result = exporter.export(processedSpans)
            result.join(5, TimeUnit.SECONDS)
            if (!result.isSuccess) {
                LOG.warn("Failed to export spans to Honeycomb: $result")
            }
        } catch (e: Exception) {
            LOG.warn("Error exporting spans to Honeycomb", e)
        }
    }

    private fun processSpans(spans: Collection<SpanData>): Collection<SpanData> {
        val filteredSpans = mutableListOf<SpanData>()

        for (span in spans) {
            if (span.name == "application-init") {
                synchronized(eventsLock) {
                    defaultSpanEvents.addAll(span.events)
                }
                LOG.debug("Captured ${span.events.size} events from default span, filtering it out from export")
            } else {
                filteredSpans.add(span)
            }
        }

        return attachSessionParentAndEvents(filteredSpans)
    }

    private fun attachSessionParentAndEvents(spans: Collection<SpanData>): Collection<SpanData> {
        val spanId = sessionSpanId
        val traceId = sessionTraceId
        if (spanId == null || traceId == null) {
            return spans
        }

        val sessionSpanContext = SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault())
        val defaultEvents = extractDefaultSpanEvents()

        return spans.map { transformSpan(it, sessionSpanContext, defaultEvents) }
    }

    private fun transformSpan(span: SpanData, sessionSpanContext: SpanContext, defaultEvents: List<EventData>): SpanData {
        val isSessionSpan = span.name == "remote-dev-session"

        return when {
            isSessionSpan && defaultEvents.isNotEmpty() -> {
                LOG.debug("Adding ${defaultEvents.size} events from default span to session span")
                SpanDelegatingData(span, span.parentSpanContext, defaultEvents)
            }
            !span.parentSpanContext.isValid && !isSessionSpan -> {
                SpanDelegatingData(span, sessionSpanContext, emptyList())
            }
            else -> span
        }
    }

    private fun extractDefaultSpanEvents(): List<EventData> {
        if (defaultSpanEvents.isEmpty()) return emptyList()

        return synchronized(eventsLock) {
            if (defaultSpanEvents.isEmpty()) return emptyList()
            val events = defaultSpanEvents.toList()
            defaultSpanEvents.clear()
            events
        }
    }

    private fun filterSpans(spans: Collection<SpanData>): Collection<SpanData> {
        val filterService = SpanFilterService.getInstance()
        return spans.filter { filterService.isSpanEnabled(it.name) }
    }

    private class SpanDelegatingData(
        private val delegate: SpanData,
        private val newParent: SpanContext,
        private val additionalEvents: List<EventData>
    ) : SpanData by delegate {
        override fun getTraceId(): String? = delegate.traceId
        override fun getSpanId(): String? = delegate.spanId
        override fun getParentSpanContext(): SpanContext = newParent
        override fun getParentSpanId(): String? = delegate.parentSpanId
        override fun getInstrumentationScopeInfo(): InstrumentationScopeInfo? = delegate.instrumentationScopeInfo
        override fun getEvents(): List<EventData> {
            return if (additionalEvents.isEmpty()) {
                delegate.events
            } else {
                delegate.events + additionalEvents
            }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(TelemetrySpanExporter::class.java)
        fun getInstance(): TelemetrySpanExporter = service()
    }
}
