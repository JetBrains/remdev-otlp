package com.jetbrains.otp.connection

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
class ExpectedDisconnectTracker {
    private var expectedDisconnect: ExpectedDisconnect? = null

    @Synchronized
    fun mark(reason: ExpectedDisconnectReason, ttlMillis: Long = DEFAULT_TTL_MILLIS) {
        expectedDisconnect = ExpectedDisconnect(
            reason = reason,
            validUntilMillis = System.currentTimeMillis() + ttlMillis.coerceAtLeast(0L),
        )
    }

    @Synchronized
    fun currentReason(nowMillis: Long = System.currentTimeMillis()): ExpectedDisconnectReason? {
        val current = expectedDisconnect ?: return null
        if (nowMillis > current.validUntilMillis) {
            expectedDisconnect = null
            return null
        }
        return current.reason
    }

    @Synchronized
    fun clear() {
        expectedDisconnect = null
    }

    private data class ExpectedDisconnect(
        val reason: ExpectedDisconnectReason,
        val validUntilMillis: Long,
    )

    companion object {
        const val DEFAULT_TTL_MILLIS = 30_000L
        const val SHUTDOWN_TTL_MILLIS = 60_000L

        fun getInstance(): ExpectedDisconnectTracker = service()
    }
}
