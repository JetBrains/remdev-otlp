package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.TimeUnit

interface OtlpConfig {
    suspend fun initialize()

    val endpoint: String
    val headers: Map<String, String>
    val timeoutSeconds: Long
}

class FromEnvOtlpConfig(
    override val endpoint: String = System.getProperty("otel.exporter.otlp.endpoint")
        ?: System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT")
        ?: "https://api.honeycomb.io",
    override val timeoutSeconds: Long = 10
) : OtlpConfig {
    override var headers: Map<String, String> = emptyMap()
        private set

    override suspend fun initialize() {
        val headersStr = System.getProperty("otel.exporter.otlp.headers")
            ?: System.getenv("OTEL_EXPORTER_OTLP_HEADERS")
        headers = parseOtlpHeaders(headersStr)
    }
}

fun parseOtlpHeaders(headersStr: String?): Map<String, String> {
    if (headersStr.isNullOrBlank()) return emptyMap()
    return headersStr.split(",").mapNotNull { entry ->
        val parts = entry.split("=", limit = 2)
        if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
    }.toMap()
}

object OtlpSpanExporterFactory {
    private val LOG = Logger.getInstance(OtlpSpanExporterFactory::class.java)

    suspend fun create(config: OtlpConfig): SpanExporter? {
        config.initialize()

        if (config.headers.isEmpty()) {
            LOG.warn("OTLP headers not configured. Set OTEL_EXPORTER_OTLP_HEADERS environment variable or otel.exporter.otlp.headers system property.")
            return null
        }

        return try {
            val builder = OtlpHttpSpanExporter.builder()
                .setEndpoint("${config.endpoint}/v1/traces")
                .setTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            config.headers.forEach { (key, value) -> builder.addHeader(key, value) }
            builder.build()
        } catch (e: Exception) {
            LOG.error("Failed to initialize OTLP span exporter", e)
            null
        }
    }
}