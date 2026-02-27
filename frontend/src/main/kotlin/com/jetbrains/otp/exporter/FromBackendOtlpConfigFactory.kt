package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.crypto.FrontendCryptoClient
import com.jetbrains.otp.crypto.rpc.CryptoRpc

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
            val headersStr = cryptoClient.decryptData(remoteConfig.encryptedHeaders)
            return OtlpConfig(
                endpoint = remoteConfig.endpoint,
                headers = parseOtlpHeaders(headersStr),
                timeoutSeconds = timeoutSeconds,
                isPluginSpanFilterEnabled = remoteConfig.isPluginSpanFilterEnabled
            )
        } catch (e: Exception) {
            LOG.error("Failed to initialize OTLP config from backend", e)
            throw e
        }
    }

    companion object {
        private val LOG = Logger.getInstance(FromBackendOtlpConfigFactory::class.java)
    }
}