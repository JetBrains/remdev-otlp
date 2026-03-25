package com.jetbrains.otp.exporter

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.net.ssl.CertificateManager
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

internal object OtlpSslConfigurator {
    private val LOG = Logger.getInstance(OtlpSslConfigurator::class.java)

    fun configureIfSecure(endpoint: String, configure: (SSLContext, X509TrustManager) -> Unit) {
        if (!endpoint.startsWith("https://", ignoreCase = true)) return

        try {
            val certificateManager = CertificateManager.getInstance()
            configure(certificateManager.sslContext, certificateManager.trustManager)
        } catch (e: Exception) {
            LOG.warn(
                "Failed to reuse IntelliJ SSL context for OTLP exporter. Falling back to OpenTelemetry defaults.",
                e
            )
        }
    }
}
