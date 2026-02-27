package com.jetbrains.otp.crypto

import com.jetbrains.otp.crypto.rpc.CryptoRpc
import com.jetbrains.otp.exporter.readOtlpEndpointFromPropertyOrEnv
import com.jetbrains.otp.exporter.readPluginSpanFilterEnabledFromPropertyOrEnv

internal class CryptoRpcImpl : CryptoRpc {
    private val cryptoService = BackendCryptoService.getInstance()

    override suspend fun requestKeyExchange(clientPublicKey: String): EncryptedAesKey {
        return cryptoService.encryptAesKeyForClient(clientPublicKey)
    }

    override suspend fun sendEncryptedData(data: EncryptedData): String {
        return cryptoService.decryptData(data)
    }

    override suspend fun getOtlpRemoteConfig(): OtlpRemoteConfig {
        val headersStr = System.getProperty("otel.exporter.otlp.headers")
            ?: System.getenv("OTEL_EXPORTER_OTLP_HEADERS")
            ?: ""
        return OtlpRemoteConfig(
            endpoint = readOtlpEndpointFromPropertyOrEnv(),
            encryptedHeaders = cryptoService.encryptData(headersStr),
            isPluginSpanFilterEnabled = readPluginSpanFilterEnabledFromPropertyOrEnv()
        )
    }
}