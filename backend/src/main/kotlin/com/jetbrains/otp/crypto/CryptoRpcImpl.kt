package com.jetbrains.otp.crypto

import com.jetbrains.otp.crypto.rpc.CryptoRpc
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
        return OtlpRemoteConfig(
            endpoint = readOtlpEndpointFromPropertyOrEnv(),
            encryptedHeaders = cryptoService.encryptData(headersStr),
            isPluginSpanFilterEnabled = OtpDiagnosticSettings.getInstance().isPluginSpanFilterEnabledEffective(),
        )
    }
}
