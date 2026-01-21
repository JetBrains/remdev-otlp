package com.jetbrains.otp.span

import com.intellij.platform.rpc.backend.RemoteApiProvider
import com.jetbrains.otp.span.api.OtpSessionSpanApi
import fleet.rpc.remoteApiDescriptor

internal class OtpSessionSpanApiProvider : RemoteApiProvider {
    override fun RemoteApiProvider.Sink.remoteApis() {
        remoteApi(remoteApiDescriptor<OtpSessionSpanApi>()) {
            BackendOtpSessionSpanApiImpl()
        }
    }
}
