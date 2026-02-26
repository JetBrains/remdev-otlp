package com.jetbrains.otp.span

import com.jetbrains.otp.api.OtpSessionSpanApi
import com.jetbrains.otp.exporter.processor.SessionProcessor

internal class BackendOtpSessionSpanApiImpl : OtpSessionSpanApi {
    override suspend fun notifySessionSpanInitialized(
        spanId: String,
        traceId: String,
        commonSpanAttributes: Map<String, String>
    ) {
        CommonSpanAttributesState.upsert(commonSpanAttributes)
        CommonSpanAttributesState.put(CommonSpanAttributes.RD_SIDE, CommonSpanAttributes.SIDE_BACKEND)
        CommonSpanAttributesState.put(CommonSpanAttributes.PROCESS_ID, ProcessHandle.current().pid().toString())
        SessionProcessor.onSessionInitialized(spanId, traceId)
    }
}
