package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.settings.OtpDiagnosticSettings
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.TimeUnit

data class OtlpConfig(
    val endpoint: String,
    val headers: Map<String, String>,
    val timeoutSeconds: Long,
    val isPluginSpanFilterEnabled: Boolean,
    val isMetricsExportEnabled: Boolean
)

object OtlpConfigFactory {
    fun fromEnv(timeoutSeconds: Long = 10): OtlpConfig {
        return OtlpConfig(
            endpoint = readOtlpEndpointFromPropertyOrEnv(),
            headers = readOtlpHeadersFromPropertyOrEnv(),
            timeoutSeconds = timeoutSeconds,
            isPluginSpanFilterEnabled = OtpDiagnosticSettings.getInstance().pluginFilterEnabledEffective(),
            isMetricsExportEnabled = OtpDiagnosticSettings.getInstance().metricsExportEnabledEffective()
        )
    }
}

const val OTLP_ENDPOINT_PROPERTY = "otel.exporter.otlp.endpoint"
const val OTLP_ENDPOINT_ENV = "OTEL_EXPORTER_OTLP_ENDPOINT"
const val OTLP_HEADERS_PROPERTY = "otel.exporter.otlp.headers"
const val OTLP_HEADERS_ENV = "OTEL_EXPORTER_OTLP_HEADERS"
const val DEFAULT_OTLP_ENDPOINT = "http://localhost"

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
const val METRICS_EXPORT_ENABLED_PROPERTY = "rdct.diagnostic.otlp.metrics.enabled"
const val METRICS_EXPORT_ENABLED_ENV = "RDCT_DIAGNOSTIC_OTLP_METRICS_ENABLED"
const val FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED_PROPERTY =
    "rdct.diagnostic.otlp.frequent.performance.metrics.reporting.enabled"
const val FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED_ENV =
    "RDCT_DIAGNOSTIC_OTLP_FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED"

const val METRICS_DENYLIST_ENABLED_PROPERTY = "rdct.diagnostic.otlp.metrics.denylist.enabled"
const val METRICS_DENYLIST_ENABLED_ENV = "RDCT_DIAGNOSTIC_OTLP_METRICS_DENYLIST_ENABLED"

const val METRICS_EXPORT_INTERVAL_MINUTES_PROPERTY = "rdct.diagnostic.otlp.metrics.export.interval.minutes"
const val METRICS_EXPORT_INTERVAL_MINUTES_ENV = "RDCT_DIAGNOSTIC_OTLP_METRICS_EXPORT_INTERVAL_MINUTES"

fun readPluginFilterEnabled(defaultValue: Boolean = true): Boolean {
    return readBooleanFromPropertyOrEnv(
        propertyName = PLUGIN_SPAN_FILTER_ENABLED_PROPERTY,
        envName = PLUGIN_SPAN_FILTER_ENABLED_ENV,
        defaultValue = defaultValue
    )
}

fun hasPluginFilterOverride(): Boolean {
    return System.getProperty(PLUGIN_SPAN_FILTER_ENABLED_PROPERTY) != null
        || System.getenv(PLUGIN_SPAN_FILTER_ENABLED_ENV) != null
}

fun readMetricsExportEnabled(defaultValue: Boolean = true): Boolean {
    return readBooleanFromPropertyOrEnv(
        propertyName = METRICS_EXPORT_ENABLED_PROPERTY,
        envName = METRICS_EXPORT_ENABLED_ENV,
        defaultValue = defaultValue
    )
}

fun hasMetricsExportOverride(): Boolean {
    return System.getProperty(METRICS_EXPORT_ENABLED_PROPERTY) != null
        || System.getenv(METRICS_EXPORT_ENABLED_ENV) != null
}

fun readFrequentPerformanceMetricsReportingEnabled(defaultValue: Boolean = false): Boolean {
    val rawValue = System.getProperty(FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED_PROPERTY)
        ?: System.getenv(FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED_ENV)
        ?: return defaultValue
    return parseBoolean(rawValue) ?: defaultValue
}

fun hasFrequentPerformanceMetricsReportingOverride(): Boolean {
    return System.getProperty(FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED_PROPERTY) != null
        || System.getenv(FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED_ENV) != null
}

fun readMetricsDenylistEnabled(defaultValue: Boolean = true): Boolean {
    return readBooleanFromPropertyOrEnv(
        propertyName = METRICS_DENYLIST_ENABLED_PROPERTY,
        envName = METRICS_DENYLIST_ENABLED_ENV,
        defaultValue = defaultValue
    )
}

fun hasMetricsDenylistOverride(): Boolean {
    return System.getProperty(METRICS_DENYLIST_ENABLED_PROPERTY) != null
        || System.getenv(METRICS_DENYLIST_ENABLED_ENV) != null
}

fun readMetricsExportIntervalMinutes(defaultValue: Int = 5): Int {
    val rawValue = System.getProperty(METRICS_EXPORT_INTERVAL_MINUTES_PROPERTY)
        ?: System.getenv(METRICS_EXPORT_INTERVAL_MINUTES_ENV)
        ?: return defaultValue
    return rawValue.toIntOrNull()?.coerceIn(1, 60) ?: defaultValue
}

fun hasMetricsExportIntervalOverride(): Boolean {
    return System.getProperty(METRICS_EXPORT_INTERVAL_MINUTES_PROPERTY) != null
        || System.getenv(METRICS_EXPORT_INTERVAL_MINUTES_ENV) != null
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
