package com.jetbrains.otp.crypto

import com.jetbrains.otp.exporter.OtlpProtocol
import kotlinx.serialization.Serializable

@Serializable
data class OtlpRemoteConfig(
    val endpoint: String,
    val encryptedHeaders: EncryptedData,
    val encryptedTraceHeaders: EncryptedData? = null,
    val encryptedMetricHeaders: EncryptedData? = null,
    val configuredSpanAttributes: Map<String, String> = emptyMap(),
    val protocol: OtlpProtocol? = null,
    val pluginFilterOverride: Boolean?,
    val metricsExportOverride: Boolean?,
    val frequentPerformanceMetricsReportingOverride: Boolean? = null,
)
