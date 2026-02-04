package com.jetbrains.otp.span

import com.jetbrains.otp.api.OtpSessionSpanApi
import com.jetbrains.otp.exporter.processor.SessionProcessor

internal class BackendOtpSessionSpanApiImpl : OtpSessionSpanApi {
    override suspend fun notifySessionSpanInitialized(spanId: String, traceId: String) {
        SessionProcessor.onSessionInitialized(spanId, traceId)
    }
}
