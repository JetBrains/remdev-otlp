package com.jetbrains.otp.crypto

import com.jetbrains.otp.crypto.rpc.CryptoRpc

internal class CryptoRpcImpl : CryptoRpc {
    private val cryptoService = BackendCryptoService.getInstance()

    override suspend fun requestKeyExchange(clientPublicKey: String): EncryptedAesKey {
        return cryptoService.encryptAesKeyForClient(clientPublicKey)
    }

    override suspend fun sendEncryptedData(data: EncryptedData): String {
        return cryptoService.decryptData(data)
    }

    override suspend fun getEncryptedOtlpHeaders(): EncryptedData {
        val headersStr = System.getProperty("otel.exporter.otlp.headers")
            ?: System.getenv("OTEL_EXPORTER_OTLP_HEADERS")
            ?: ""
        return cryptoService.encryptData(headersStr)
    }
}