package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.TimeUnit

data class OtlpConfig(
    val apiKey: String?,
    val dataset: String,
    val endpoint: String,
    val timeoutSeconds: Long = 10
) {
    companion object {
        fun fromEnvironment(): OtlpConfig = OtlpConfig(
            apiKey = System.getProperty("honeycomb.api.key")
                ?: System.getenv("HONEYCOMB_API_KEY"),
            dataset = System.getProperty("honeycomb.dataset")
                ?: System.getenv("HONEYCOMB_DATASET")
                ?: "intellij-plugin",
            endpoint = "https://api.honeycomb.io/v1/traces"
        )
    }
}

object OtlpSpanExporterFactory {
    private val LOG = Logger.getInstance(OtlpSpanExporterFactory::class.java)

    fun create(config: OtlpConfig = OtlpConfig.fromEnvironment()): SpanExporter? {
        if (config.apiKey.isNullOrBlank()) {
            LOG.warn("Honeycomb API key not configured. Set HONEYCOMB_API_KEY environment variable or honeycomb.api.key system property.")
            return null
        }

        return try {
            OtlpHttpSpanExporter.builder()
                .setEndpoint(config.endpoint)
                .addHeader("x-honeycomb-team", config.apiKey)
                .addHeader("x-honeycomb-dataset", config.dataset)
                .setTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            LOG.error("Failed to initialize Honeycomb exporter", e)
            null
        }
    }
}