package com.jetbrains.otp.exporter

import com.intellij.application.subscribe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.diagnostic.telemetry.impl.TelemetryReceivedListener
import com.jetbrains.otp.settings.SpanFilterService
import com.jetbrains.otp.span.SessionSpanListener
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
class TelemetrySpanExporter {
    private val spanExporter: SpanExporter? by lazy { OtlpSpanExporterFactory.create() }
    @Volatile
    private var isSessionInitialized = false
    private val bufferedSpans = CopyOnWriteArrayList<SpanData>()

    fun sendSpans(spanData: Collection<SpanData>) {
        exportSpans(spanData)
    }

    fun sessionSpanInitialized(spanId: String, traceId: String) {
        val spansToFlush = synchronized(this) {
            if (isSessionInitialized) {
                LOG.debug("Session already initialized, ignoring duplicate call")
                return
            }
            isSessionInitialized = true
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

        val spansToExport = synchronized(this) {
            bufferedSpans.addAll(filteredSpans)
            if (isSessionInitialized) {
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

        try {
            val result = exporter.export(spans)
            result.join(5, TimeUnit.SECONDS)
            if (!result.isSuccess) {
                LOG.warn("Failed to export spans to Honeycomb: $result")
            }
        } catch (e: Exception) {
            LOG.warn("Error exporting spans to Honeycomb", e)
        }
    }

    private fun filterSpans(spans: Collection<SpanData>): Collection<SpanData> {
        val filterService = SpanFilterService.getInstance()
        return spans.filter { filterService.isSpanEnabled(it.name) }
    }

    companion object {
        private val LOG = Logger.getInstance(TelemetrySpanExporter::class.java)
        fun getInstance(): TelemetrySpanExporter = service()
    }
}
