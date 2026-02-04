package com.jetbrains.otp.connection

import com.intellij.openapi.client.ClientAppSession
import com.intellij.platform.frontend.split.connection.ConnectionInfoProvider
import com.jetbrains.otp.span.DefaultRootSpanService
import com.jetbrains.otp.api.OtpSessionSpanApi
import com.jetbrains.otp.exporter.processor.SessionProcessor
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rdclient.connection.ClientSessionListener
import com.jetbrains.thinclient.ThinClientId
import kotlinx.coroutines.launch

class ClientSessionSpanUpdater : ClientSessionListener {
    @Suppress("UnstableApiUsage")
    override fun appSessionInitialized(
        lifetime: Lifetime,
        session: ClientAppSession
    ) {
        val sessionSpan = DefaultRootSpanService.getInstance().startSessionSpan(ThinClientId.Instance.value)
        val hostName = ConnectionInfoProvider.getBackendName()
        val sessionId = session.clientId.value
        DefaultRootSpanService.currentSpan().setAttribute("host-name", hostName)
        DefaultRootSpanService.currentSpan().setAttribute("session.id", sessionId)
        SessionProcessor.onSessionInitialized(
            sessionSpan.spanContext.spanId,
            sessionSpan.spanContext.traceId,
            hostName,
            sessionId
        )

        notifyBackendAboutSessionStart(sessionSpan.spanContext.spanId, sessionSpan.spanContext.traceId)

        lifetime.onTermination {
            DefaultRootSpanService.getInstance().endSessionSpan()
        }
    }

    fun notifyBackendAboutSessionStart(spanId: String, traceId: String) {
        getFrontendCoroutineScope().launch {
            OtpSessionSpanApi.getInstance().notifySessionSpanInitialized(spanId, traceId)
        }
    }
}