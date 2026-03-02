package com.jetbrains.otp.crypto

import com.jetbrains.otp.crypto.rpc.CryptoRpc
import com.jetbrains.otp.exporter.hasMetricsExportOverride
import com.jetbrains.otp.exporter.hasPluginFilterOverride
import com.jetbrains.otp.exporter.readOtlpEndpointFromPropertyOrEnv
import com.jetbrains.otp.exporter.readRawOtlpHeadersFromPropertyOrEnv
import com.jetbrains.otp.settings.OtpDiagnosticSettings

internal class CryptoRpcImpl : CryptoRpc {
    private val cryptoService = BackendCryptoService.getInstance()

    override suspend fun requestKeyExchange(clientPublicKey: String): EncryptedAesKey {
        return cryptoService.encryptAesKeyForClient(clientPublicKey)
    }

    override suspend fun sendEncryptedData(data: EncryptedData): String {
        return cryptoService.decryptData(data)
    }

    override suspend fun getOtlpRemoteConfig(): OtlpRemoteConfig {
        val headersStr = readRawOtlpHeadersFromPropertyOrEnv()
        val settings = OtpDiagnosticSettings.getInstance()
        val pluginFilterOverride = if (hasPluginFilterOverride()) settings.pluginFilterEnabledEffective() else null
        val metricsExportOverride = if (hasMetricsExportOverride()) settings.metricsExportEnabledEffective() else null
        return OtlpRemoteConfig(
            endpoint = readOtlpEndpointFromPropertyOrEnv(),
            encryptedHeaders = cryptoService.encryptData(headersStr),
            pluginFilterOverride = pluginFilterOverride,
            metricsExportOverride = metricsExportOverride,
        )
    }
}