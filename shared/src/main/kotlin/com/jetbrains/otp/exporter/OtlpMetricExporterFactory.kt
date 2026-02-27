package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.sdk.metrics.export.MetricExporter
import java.util.concurrent.TimeUnit

object OtlpMetricExporterFactory {
    private val LOG = Logger.getInstance(OtlpMetricExporterFactory::class.java)

    fun create(config: OtlpConfig): MetricExporter? {
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