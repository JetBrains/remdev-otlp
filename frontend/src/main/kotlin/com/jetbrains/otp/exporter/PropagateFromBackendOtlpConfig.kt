package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.crypto.FrontendCryptoClient
import com.jetbrains.otp.crypto.rpc.CryptoRpc

class PropagateFromBackendOtlpConfig(
    override var endpoint: String = DEFAULT_OTLP_ENDPOINT,
    override val timeoutSeconds: Long = 10
) : OtlpConfig {
    private val cryptoClient = FrontendCryptoClient.getInstance()
    override var headers: Map<String, String> = emptyMap()
        private set
    override var isPluginSpanFilterEnabled: Boolean = readPluginSpanFilterEnabledFromPropertyOrEnv()
        private set

    private var initialized = false

    override suspend fun initialize() {
        if (initialized) return
        try {
            if (!cryptoClient.isInitialized()) {
                cryptoClient.initialize()
            }

            val cryptoRpc = CryptoRpc.getInstance()
            val remoteConfig = cryptoRpc.getOtlpRemoteConfig()
            endpoint = remoteConfig.endpoint
            val headersStr = cryptoClient.decryptData(remoteConfig.encryptedHeaders)
            headers = parseOtlpHeaders(headersStr)
            isPluginSpanFilterEnabled = remoteConfig.isPluginSpanFilterEnabled
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