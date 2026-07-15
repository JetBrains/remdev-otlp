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
            validUntilMillis = validUntilMillis(ttlMillis),
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

    private fun validUntilMillis(ttlMillis: Long): Long {
        val nowMillis = System.currentTimeMillis()
        val safeTtlMillis = ttlMillis.coerceAtLeast(0L)
        return if (Long.MAX_VALUE - nowMillis < safeTtlMillis) {
            Long.MAX_VALUE
        } else {
            nowMillis + safeTtlMillis
        }
    }

    companion object {
        const val DEFAULT_TTL_MILLIS = 30_000L
        const val SHUTDOWN_TTL_MILLIS = 60_000L
        const val SYSTEM_SLEEP_TTL_MILLIS = 30 * 60 * 1000L
        const val REMOTE_SYSTEM_SLEEP_TTL_MILLIS = SYSTEM_SLEEP_TTL_MILLIS
        const val SYSTEM_WAKE_TTL_MILLIS = 2 * 60 * 1000L

        fun getInstance(): ExpectedDisconnectTracker = service()
    }
}
