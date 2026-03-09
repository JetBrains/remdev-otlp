package com.jetbrains.otp.crypto

import com.jetbrains.otp.exporter.OtlpProtocol
import kotlinx.serialization.Serializable

@Serializable
data class OtlpRemoteConfig(
    val endpoint: String,
    val protocol: OtlpProtocol,
    val encryptedHeaders: EncryptedData,
    val pluginFilterOverride: Boolean?,
    val metricsExportOverride: Boolean?,
    val frequentPerformanceMetricsReportingOverride: Boolean? = null,
)
