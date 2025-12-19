package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.diagnostic.telemetry.impl.TelemetryReceivedListener
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.TimeUnit

@Suppress("UnstableApiUsage")
class TelemetrySpanExporter : TelemetryReceivedListener {
    companion object {
        private val LOG = Logger.getInstance(TelemetrySpanExporter::class.java)
    }

    private val spanExporter: SpanExporter? by lazy {
        OtlpSpanExporterFactory.create()
    }

    override fun sendSpans(spanData: Collection<SpanData>) {
        val exporter = spanExporter
        if (exporter == null) {
            LOG.debug("Honeycomb exporter not initialized. Spans will not be sent.")
            return
        }

        try {
            val result = exporter.export(spanData)
            result.join(2, TimeUnit.SECONDS)
            if (!result.isSuccess) {
                LOG.warn("Failed to export spans to Honeycomb: $result")
            }
        } catch (e: Exception) {
            LOG.warn("Error exporting spans to Honeycomb", e)
        }
    }
}