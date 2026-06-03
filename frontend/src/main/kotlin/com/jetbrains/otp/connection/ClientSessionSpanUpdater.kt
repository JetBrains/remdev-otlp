package com.jetbrains.otp.connection

import com.intellij.openapi.client.ClientAppSession
import com.intellij.platform.frontend.split.connection.ConnectionInfoProvider
import com.intellij.platform.split.connection.impl.ConnectionFlowService
import com.intellij.util.fragmentParameters
import com.jetbrains.otp.api.OtpSessionSpanApi
import com.jetbrains.otp.exporter.processor.SessionProcessor
import com.jetbrains.otp.span.CommonSpanAttributes
import com.jetbrains.otp.span.CommonSpanAttributesState
import com.jetbrains.otp.span.DefaultRootSpanService
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rdclient.connection.ClientSessionListener
import kotlinx.coroutines.launch

class ClientSessionSpanUpdater : ClientSessionListener {
    @Suppress("UnstableApiUsage")
    override fun appSessionInitialized(
        lifetime: Lifetime,
        session: ClientAppSession
    ) {
        val connectionSessionId = ConnectionInfoProvider.getSessionId()
        val sessionId = rdSessionId() ?: connectionSessionId
        val sessionSpan = DefaultRootSpanService.getInstance().startSessionSpan(sessionId)
        val processId = ProcessHandle.current().pid().toString()
        CommonSpanAttributesState.put(CommonSpanAttributes.SESSION_ID, sessionId)
        CommonSpanAttributesState.put(CommonSpanAttributes.PROCESS_ID, processId)

        SessionProcessor.onSessionInitialized(
            sessionSpan.spanContext.spanId,
            sessionSpan.spanContext.traceId
        )

        notifyBackendAboutSessionStart(
            sessionSpan.spanContext.spanId,
            sessionSpan.spanContext.traceId,
            CommonSpanAttributesState.snapshotMap() + (CommonSpanAttributes.HOST_NAME to ConnectionInfoProvider.getBackendName())
        )

        lifetime.onTermination {
            DefaultRootSpanService.getInstance().endSessionSpan()
        }
    }

    private fun rdSessionId(): String? {
        return runCatching {
            ConnectionFlowService.getInstance().originalUrl?.fragmentParameters?.get("fp")
        }.getOrNull()
    }

    fun notifyBackendAboutSessionStart(spanId: String, traceId: String, commonSpanAttributes: Map<String, String>) {
        getFrontendCoroutineScope().launch {
            OtpSessionSpanApi.getInstance().notifySessionSpanInitialized(spanId, traceId, commonSpanAttributes)
        }
    }
}
