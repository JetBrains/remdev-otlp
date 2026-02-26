package com.jetbrains.otp.api

import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

@Rpc
@Suppress("UnstableApiUsage")
@ApiStatus.Internal
interface OtpSessionSpanApi : RemoteApi<Unit> {
    suspend fun notifySessionSpanInitialized(spanId: String, traceId: String, commonSpanAttributes: Map<String, String>)

    companion object {
        @JvmStatic
        suspend fun getInstance(): OtpSessionSpanApi {
            return RemoteApiProviderService.resolve(remoteApiDescriptor<OtpSessionSpanApi>())
        }
    }
}
