package com.jetbrains.otp.connection

import com.intellij.platform.rpc.backend.RemoteApiProvider
import com.jetbrains.otp.api.ExpectedDisconnectApi
import fleet.rpc.remoteApiDescriptor

internal class ExpectedDisconnectApiProvider : RemoteApiProvider {
    override fun RemoteApiProvider.Sink.remoteApis() {
        remoteApi(remoteApiDescriptor<ExpectedDisconnectApi>()) {
            BackendExpectedDisconnectApiImpl()
        }
    }
}
