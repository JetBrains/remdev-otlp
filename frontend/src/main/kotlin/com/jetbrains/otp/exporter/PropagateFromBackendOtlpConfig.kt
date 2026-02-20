package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.crypto.FrontendCryptoClient
import com.jetbrains.otp.crypto.rpc.CryptoRpc

class PropagateFromBackendOtlpConfig(
    override val endpoint: String = System.getProperty("otel.exporter.otlp.endpoint")
        ?: System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT")
        ?: "https://api.honeycomb.io",
    override val timeoutSeconds: Long = 10
) : OtlpConfig {
    private val cryptoClient = FrontendCryptoClient.getInstance()
    override var headers: Map<String, String> = emptyMap()
        private set

    private var initialized = false

    override suspend fun initialize() {
        if (initialized) return
        try {
            if (!cryptoClient.isInitialized()) {
                cryptoClient.initialize()
            }

            val cryptoRpc = CryptoRpc.getInstance()
            val encryptedHeaders = cryptoRpc.getEncryptedOtlpHeaders()
            val headersStr = cryptoClient.decryptData(encryptedHeaders)
            headers = parseOtlpHeaders(headersStr)
            initialized = true
        } catch (e: Exception) {
            LOG.error("Failed to initialize OTLP config from backend", e)
            throw e
        }
    }

    companion object {
        private val LOG = Logger.getInstance(PropagateFromBackendOtlpConfig::class.java)
    }
}