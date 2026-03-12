package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.settings.OtpDiagnosticSettings
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.trace.export.SpanExporter
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

@Serializable
enum class OtlpProtocol {
    HTTP_PROTOBUF,
    GRPC
}

data class OtlpConfig(
    val endpoint: String,
    val headers: Map<String, String>,
    val protocol: OtlpProtocol,
    val timeoutSeconds: Long,
    val isPluginSpanFilterEnabled: Boolean,
    val isMetricsExportEnabled: Boolean
)

object OtlpConfigFactory {
    fun fromEnv(timeoutSeconds: Long = 10): OtlpConfig {
        return OtlpConfig(
            endpoint = readOtlpEndpointFromPropertyOrEnv(),
            headers = readOtlpHeadersFromPropertyOrEnv(),
            protocol = readOtlpProtocolFromPropertyOrEnv(),
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
const val OTLP_PROTOCOL_PROPERTY = "otel.exporter.otlp.protocol"
const val OTLP_PROTOCOL_ENV = "OTEL_EXPORTER_OTLP_PROTOCOL"
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

fun readOtlpProtocolFromPropertyOrEnv(defaultValue: OtlpProtocol = OtlpProtocol.HTTP_PROTOBUF): OtlpProtocol {
    val rawValue = System.getProperty(OTLP_PROTOCOL_PROPERTY)
        ?: System.getenv(OTLP_PROTOCOL_ENV)
        ?: return defaultValue
    return parseOtlpProtocol(rawValue) ?: defaultValue
}

fun parseOtlpHeaders(headersStr: String?): Map<String, String> {
    if (headersStr.isNullOrBlank()) return emptyMap()
    return headersStr.split(",").mapNotNull { entry ->
        val parts = entry.split("=", limit = 2)
        if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
    }.toMap()
}

fun parseOtlpProtocol(protocolStr: String?): OtlpProtocol? {
    return when (protocolStr?.trim()?.lowercase()) {
        "grpc" -> OtlpProtocol.GRPC
        "http/protobuf", "http-protobuf", "http" -> OtlpProtocol.HTTP_PROTOBUF
        else -> null
    }
}

const val PLUGIN_SPAN_FILTER_ENABLED_PROPERTY = "rdct.diagnostic.otlp.plugin.span.filter.enabled"
const val PLUGIN_SPAN_FILTER_ENABLED_ENV = "RDCT_DIAGNOSTIC_OTLP_PLUGIN_SPAN_FILTER_ENABLED"
const val METRICS_EXPORT_ENABLED_PROPERTY = "rdct.diagnostic.otlp.metrics.enabled"
const val METRICS_EXPORT_ENABLED_ENV = "RDCT_DIAGNOSTIC_OTLP_METRICS_ENABLED"
const val FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED_PROPERTY =
    "rdct.diagnostic.otlp.frequent.performance.metrics.reporting.enabled"
const val FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED_ENV =
    "RDCT_DIAGNOSTIC_OTLP_FREQUENT_PERFORMANCE_METRICS_REPORTING_ENABLED"

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
            when (config.protocol) {
                OtlpProtocol.HTTP_PROTOBUF -> {
                    val builder = OtlpHttpSpanExporter.builder()
                        .setEndpoint(buildHttpSignalEndpoint(config.endpoint, "/v1/traces"))
                        .setTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
                    config.headers.forEach { (key, value) -> builder.addHeader(key, value) }
                    builder.build()
                }

                OtlpProtocol.GRPC -> {
                    val builder = OtlpGrpcSpanExporter.builder()
                        .setEndpoint(config.endpoint)
                        .setTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
                    config.headers.forEach { (key, value) -> builder.addHeader(key, value) }
                    builder.build()
                }
            }
        } catch (e: Exception) {
            LOG.error("Failed to initialize OTLP span exporter", e)
            null
        }
    }

    private fun buildHttpSignalEndpoint(baseEndpoint: String, signalPath: String): String {
        return "${baseEndpoint.trim().trimEnd('/')}$signalPath"
    }
}
