package com.jetbrains.otp.exporter

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.exporter.processor.CommonAttributesProcessor
import com.jetbrains.otp.exporter.processor.PluginSpanFilterProcessor
import com.jetbrains.otp.exporter.processor.SessionProcessor
import com.jetbrains.otp.exporter.processor.SpanProcessor
import com.jetbrains.otp.exporter.processor.SpanProcessorProvider
import com.jetbrains.otp.settings.SpanFilterService
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
class TelemetrySpanExporter {
    private val processors = createProcessors()
    @Volatile
    private var currentConfig: OtlpConfig = FromEnvOtlpConfig()

    @Volatile
    private var spanExporter: SpanExporter? = null

    suspend fun initExporter(config: OtlpConfig) {
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
            SessionProcessor,
            CommonAttributesProcessor
        )

        SpanProcessorProvider.EP_NAME.extensionList.forEach { provider ->
            processors.addAll(provider.getProcessors())
        }
        return processors.sortedBy { it.getOrder() }
    }

    fun sendSpans(spanData: Collection<SpanData>) {
        val filteredSpans = filterSpans(spanData)
        if (filteredSpans.isEmpty()) return

        val processedSpans = processSpans(filteredSpans)

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

    private fun filterSpans(spans: Collection<SpanData>): Collection<SpanData> {
        val filterService = SpanFilterService.getInstance()
        return spans.filter { filterService.isSpanEnabled(it.name) }
    }

    companion object {
        private val LOG = Logger.getInstance(TelemetrySpanExporter::class.java)
        fun getInstance(): TelemetrySpanExporter = service()
    }
}
