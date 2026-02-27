package com.jetbrains.otp.api

import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

@Rpc
@Suppress("UnstableApiUsage")
@ApiStatus.Internal
interface OtpDiagnosticSettingsApi : RemoteApi<Unit> {
    suspend fun syncFilteringSettings(
        disabledCategories: Set<String>,
        frequentSpansEnabled: Boolean,
        pluginSpanFilterEnabled: Boolean,
    )

    companion object {
        @JvmStatic
        suspend fun getInstance(): OtpDiagnosticSettingsApi {
            return RemoteApiProviderService.resolve(remoteApiDescriptor<OtpDiagnosticSettingsApi>())
        }
    }
}