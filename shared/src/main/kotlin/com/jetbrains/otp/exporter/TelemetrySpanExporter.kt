package com.jetbrains.otp.exporter

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.exporter.processor.CommonAttributesProcessor
import com.jetbrains.otp.exporter.processor.FrequentSpanFilterProcessor
import com.jetbrains.otp.exporter.processor.PluginSpanFilterProcessor
import com.jetbrains.otp.exporter.processor.SessionProcessor
import com.jetbrains.otp.exporter.processor.SpanScopeFilterProcessor
import com.jetbrains.otp.exporter.processor.SpanProcessor
import com.jetbrains.otp.exporter.processor.SpanProcessorProvider
import com.jetbrains.otp.exporter.processor.TelemetryAttributeRenamingProcessor
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
class TelemetrySpanExporter(private val coroutineScope: CoroutineScope) {
    private val processors = createProcessors()
    private val exportedEarly = EarlyExportedSpans()
    @Volatile
    private var currentConfig: OtlpConfig = OtlpConfigFactory.fromEnv()

    @Volatile
    private var spanExporter: SpanExporter? = null

    fun initExporter(config: OtlpConfig) {
        currentConfig = config
        try {
            spanExporter = OtlpSpanExporterFactory.create(config)
        } catch (e: Exception) {
            LOG.error("Failed to initialize OTLP span exporter", e)
        }
    }

    private fun createProcessors(): List<SpanProcessor> {
        val processors = mutableListOf(
            PluginSpanFilterProcessor,
            FrequentSpanFilterProcessor,
            SpanScopeFilterProcessor,
            SessionProcessor,
            CommonAttributesProcessor,
            TelemetryAttributeRenamingProcessor
        )

        SpanProcessorProvider.EP_NAME.extensionList.forEach { provider ->
            processors.addAll(provider.getProcessors())
        }
        return processors.sortedBy { it.getOrder() }
    }

    fun sendSpans(spanData: Collection<SpanData>) {
        val pendingSpans = exportedEarly.withoutExported(spanData)
        if (pendingSpans.isEmpty()) return
        val processedSpans = processSpans(pendingSpans)

        if (processedSpans.isNotEmpty()) {
            doExport(processedSpans)
        }
    }

    /**
     * Exports a span that has just ended without waiting for the platform's span batch.
     *
     * The platform hands spans over only when a batch fills up, after a minute without new spans, or while shutting
     * down, when this exporter may no longer be able to send. A long-lived root span therefore reaches the backend
     * late or not at all. The copy the platform delivers later is dropped by [sendSpans].
     *
     * @param wait export on the calling thread; use it when the application is about to exit
     */
    fun exportEndedSpan(span: SpanData, wait: Boolean) {
        if (!exportedEarly.markExported(span)) return
        if (wait) {
            exportNow(span)
        } else {
            coroutineScope.launch(Dispatchers.IO) { exportNow(span) }
        }
    }

    private fun exportNow(span: SpanData) {
        val processedSpans = processSpans(listOf(span))
        if (processedSpans.isNotEmpty()) {
            doExport(processedSpans)
        }
    }

    private fun processSpans(spans: Collection<SpanData>): Collection<SpanData> {
        val config = currentConfig
        return processors.fold(spans) { currentSpans, processor ->
            processor.process(currentSpans, config)
        }
    }

    private fun doExport(spans: Collection<SpanData>) {
        val exporter = spanExporter
        if (exporter == null) {
            LOG.debug("OTLP exporter not initialized. Spans will not be sent.")
            return
        }

        try {
            val result = exporter.export(spans)
            result.join(5, TimeUnit.SECONDS)
            if (!result.isSuccess) {
                LOG.warn("Failed to export spans via OTLP: $result")
            }
        } catch (e: Exception) {
            LOG.warn("Error exporting spans via OTLP", e)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(TelemetrySpanExporter::class.java)
        fun getInstance(): TelemetrySpanExporter = service()
    }
}
