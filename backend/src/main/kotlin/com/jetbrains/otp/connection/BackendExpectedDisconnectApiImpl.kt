package com.jetbrains.otp.connection

import com.jetbrains.otp.api.ExpectedDisconnectApi

internal class BackendExpectedDisconnectApiImpl : ExpectedDisconnectApi {
    override suspend fun markExpectedDisconnect(reasonName: String, ttlMillis: Long) {
        val reason = ExpectedDisconnectReason.entries.singleOrNull { it.name == reasonName } ?: return
        ExpectedDisconnectTracker.getInstance().mark(reason, ttlMillis)
    }
}
