package com.jetbrains.otp.connection

import com.intellij.openapi.client.ClientAppSession
import com.intellij.platform.frontend.split.connection.ConnectionInfoProvider
import com.jetbrains.otp.span.CommonSpanAttributes
import com.jetbrains.otp.span.CommonSpanAttributesState
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
        CommonSpanAttributesState.put(CommonSpanAttributes.HOST_NAME, hostName)
        CommonSpanAttributesState.put(CommonSpanAttributes.SESSION_ID, sessionId)

        SessionProcessor.onSessionInitialized(
            sessionSpan.spanContext.spanId,
            sessionSpan.spanContext.traceId
        )

        notifyBackendAboutSessionStart(
            sessionSpan.spanContext.spanId,
            sessionSpan.spanContext.traceId,
            CommonSpanAttributesState.snapshotMap()
        )

        lifetime.onTermination {
            DefaultRootSpanService.getInstance().endSessionSpan()
        }
    }

    fun notifyBackendAboutSessionStart(spanId: String, traceId: String, commonSpanAttributes: Map<String, String>) {
        getFrontendCoroutineScope().launch {
            OtpSessionSpanApi.getInstance().notifySessionSpanInitialized(spanId, traceId, commonSpanAttributes)
        }
    }
}
