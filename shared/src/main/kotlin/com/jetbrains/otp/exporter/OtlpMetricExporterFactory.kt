package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.sdk.metrics.export.MetricExporter
import java.util.concurrent.TimeUnit

object OtlpMetricExporterFactory {
    private val LOG = Logger.getInstance(OtlpMetricExporterFactory::class.java)

    suspend fun create(config: OtlpConfig): MetricExporter? {
        config.initialize()

        if (config.headers.isEmpty()) {
            LOG.warn("OTLP headers not configured. Set OTEL_EXPORTER_OTLP_HEADERS environment variable or otel.exporter.otlp.headers system property.")
            return null
        }

        return try {
            val builder = OtlpHttpMetricExporter.builder()
                .setEndpoint("${config.endpoint}/v1/metrics")
                .setTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            config.headers.forEach { (key, value) -> builder.addHeader(key, value) }
            builder.build()
        } catch (e: Exception) {
            LOG.error("Failed to initialize OTLP metric exporter", e)
            null
        }
    }
}