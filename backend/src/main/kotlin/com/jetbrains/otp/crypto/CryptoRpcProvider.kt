package com.jetbrains.otp.crypto

import com.intellij.platform.rpc.backend.RemoteApiProvider
import com.jetbrains.otp.crypto.rpc.CryptoRpc
import fleet.rpc.remoteApiDescriptor

internal class CryptoRpcProvider : RemoteApiProvider {
    override fun RemoteApiProvider.Sink.remoteApis() {
        remoteApi(remoteApiDescriptor<CryptoRpc>()) {
            CryptoRpcImpl()
        }
    }
}