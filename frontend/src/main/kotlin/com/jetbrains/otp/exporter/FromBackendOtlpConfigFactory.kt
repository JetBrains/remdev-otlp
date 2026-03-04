package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.crypto.FrontendCryptoClient
import com.jetbrains.otp.crypto.OtlpRemoteConfig
import com.jetbrains.otp.crypto.rpc.CryptoRpc
import com.jetbrains.otp.settings.OtpDiagnosticSettings

class FromBackendOtlpConfigFactory(
    private val cryptoClient: FrontendCryptoClient = FrontendCryptoClient.getInstance()
) {
    suspend fun createConfig(timeoutSeconds: Long = 10): OtlpConfig {
        try {
            if (!cryptoClient.isInitialized()) {
                cryptoClient.initialize()
            }

            val cryptoRpc = CryptoRpc.getInstance()
            val remoteConfig = cryptoRpc.getOtlpRemoteConfig()
            val settings = updateLocalSettings(remoteConfig)
            val headersStr = cryptoClient.decryptData(remoteConfig.encryptedHeaders)
            val pluginSpanFilterEnabled = remoteConfig.pluginFilterOverride ?: settings.isPluginSpanFilterEnabled()
            val metricsExportEnabled = remoteConfig.metricsExportOverride ?: settings.isMetricsExportEnabled()
            return OtlpConfig(
                endpoint = remoteConfig.endpoint,
                headers = parseOtlpHeaders(headersStr),
                timeoutSeconds = timeoutSeconds,
                isPluginSpanFilterEnabled = pluginSpanFilterEnabled,
                isMetricsExportEnabled = metricsExportEnabled
            )
        } catch (e: Exception) {
            LOG.error("Failed to initialize OTLP config from backend", e)
            throw e
        }
    }

    private fun updateLocalSettings(remoteConfig: OtlpRemoteConfig): OtpDiagnosticSettings {
        val settings = OtpDiagnosticSettings.getInstance()
        settings.updateBackendPluginFilterOverride(remoteConfig.pluginFilterOverride)
        settings.updateBackendMetricsExportOverride(remoteConfig.metricsExportOverride)
        settings.updateBackendCpuWindowMetricsReportingOverride(remoteConfig.cpuWindowMetricsReportingOverride)
        return settings
    }

    companion object {
        private val LOG = Logger.getInstance(FromBackendOtlpConfigFactory::class.java)
    }
}
