package com.jetbrains.otp.span

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.otp.span.api.OtpSessionSpanApi

internal class BackendOtpSessionSpanApiImpl : OtpSessionSpanApi {
    override suspend fun notifySessionSpanInitialized(spanId: String, traceId: String) {
        ApplicationManager.getApplication().messageBus.syncPublisher(SessionSpanListener.TOPIC)
            .sessionSpanInitialized(spanId, traceId)
    }
}
