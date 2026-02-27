package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.TimeUnit

data class OtlpConfig(
    val endpoint: String,
    val headers: Map<String, String>,
    val timeoutSeconds: Long,
    val isPluginSpanFilterEnabled: Boolean
)

object OtlpConfigFactory {
    fun fromEnv(timeoutSeconds: Long = 10): OtlpConfig {
        return OtlpConfig(
            endpoint = readOtlpEndpointFromPropertyOrEnv(),
            headers = readOtlpHeadersFromPropertyOrEnv(),
            timeoutSeconds = timeoutSeconds,
            isPluginSpanFilterEnabled = readPluginSpanFilterEnabledFromPropertyOrEnv()
        )
    }
}

const val OTLP_ENDPOINT_PROPERTY = "otel.exporter.otlp.endpoint"
const val OTLP_ENDPOINT_ENV = "OTEL_EXPORTER_OTLP_ENDPOINT"
const val OTLP_HEADERS_PROPERTY = "otel.exporter.otlp.headers"
const val OTLP_HEADERS_ENV = "OTEL_EXPORTER_OTLP_HEADERS"
const val DEFAULT_OTLP_ENDPOINT = "https://api.honeycomb.io"

fun readOtlpEndpointFromPropertyOrEnv(defaultValue: String = DEFAULT_OTLP_ENDPOINT): String {
    return System.getProperty(OTLP_ENDPOINT_PROPERTY)
        ?: System.getenv(OTLP_ENDPOINT_ENV)
        ?: defaultValue
}

fun readRawOtlpHeadersFromPropertyOrEnv(defaultValue: String = ""): String {
    return System.getProperty(OTLP_HEADERS_PROPERTY)
        ?: System.getenv(OTLP_HEADERS_ENV)
        ?: defaultValue
}

fun readOtlpHeadersFromPropertyOrEnv(): Map<String, String> {
    return parseOtlpHeaders(readRawOtlpHeadersFromPropertyOrEnv())
}

fun parseOtlpHeaders(headersStr: String?): Map<String, String> {
    if (headersStr.isNullOrBlank()) return emptyMap()
    return headersStr.split(",").mapNotNull { entry ->
        val parts = entry.split("=", limit = 2)
        if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
    }.toMap()
}

const val PLUGIN_SPAN_FILTER_ENABLED_PROPERTY = "rdct.diagnostic.otlp.plugin.span.filter.enabled"
const val PLUGIN_SPAN_FILTER_ENABLED_ENV = "RDCT_DIAGNOSTIC_OTLP_PLUGIN_SPAN_FILTER_ENABLED"

fun readPluginSpanFilterEnabledFromPropertyOrEnv(defaultValue: Boolean = true): Boolean {
    return readBooleanFromPropertyOrEnv(
        propertyName = PLUGIN_SPAN_FILTER_ENABLED_PROPERTY,
        envName = PLUGIN_SPAN_FILTER_ENABLED_ENV,
        defaultValue = defaultValue
    )
}

private fun readBooleanFromPropertyOrEnv(propertyName: String, envName: String, defaultValue: Boolean): Boolean {
    val rawValue = System.getProperty(propertyName)
        ?: System.getenv(envName)
        ?: return defaultValue
    return parseBoolean(rawValue) ?: defaultValue
}

private fun parseBoolean(value: String): Boolean? {
    return value.trim().lowercase().toBooleanStrictOrNull()
}

object OtlpSpanExporterFactory {
    private val LOG = Logger.getInstance(OtlpSpanExporterFactory::class.java)

    fun create(config: OtlpConfig): SpanExporter? {
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