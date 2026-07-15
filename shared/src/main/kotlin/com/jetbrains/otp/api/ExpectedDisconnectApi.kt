package com.jetbrains.otp.api

import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

@Rpc
@Suppress("UnstableApiUsage")
@ApiStatus.Internal
interface ExpectedDisconnectApi : RemoteApi<Unit> {
    suspend fun markExpectedDisconnect(reasonName: String, ttlMillis: Long)

    companion object {
        @JvmStatic
        suspend fun getInstance(): ExpectedDisconnectApi {
            return RemoteApiProviderService.resolve(remoteApiDescriptor<ExpectedDisconnectApi>())
        }
    }
}
