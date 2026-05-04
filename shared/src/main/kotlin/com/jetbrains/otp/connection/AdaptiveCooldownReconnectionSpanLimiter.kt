package com.jetbrains.otp.connection

import java.util.concurrent.TimeUnit

internal class AdaptiveCooldownReconnectionSpanLimiter(
    private val nanoTime: () -> Long = System::nanoTime,
) : ReconnectionSpanLimiter {
    private var hasEmittedSpan = false
    private var activeSpanCount = 0
    private var cooldownUntilNanos = 0L
    private var cooldownLevel = 0
    private var lastThrottledAtNanos: Long? = null
    private var throttledConnectionDropCount = 0L

    @Synchronized
    override fun tryAcquire(config: ReconnectionSpanConfig): ReconnectionSpanPermit? {
        val nowNanos = nanoTime()
        resetCooldownAfterQuietPeriod(config, nowNanos)

        if (activeSpanCount > 0) {
            incrementThrottledConnectionDropCount()
            return null
        }

        if (isInsideCooldown(nowNanos)) {
            throttleAndIncreaseCooldown(config, nowNanos)
            return null
        }

        hasEmittedSpan = true
        activeSpanCount++

        val throttledCountBeforeThisSpan = throttledConnectionDropCount
        throttledConnectionDropCount = 0L
        return ReconnectionSpanPermit(throttledCountBeforeThisSpan)
    }

    @Synchronized
    override fun onSpanEnded(config: ReconnectionSpanConfig) {
        if (activeSpanCount > 0) {
            activeSpanCount--
        }
        if (activeSpanCount == 0 && hasEmittedSpan) {
            val cooldownNanos = currentCooldownNanos(config)
            cooldownUntilNanos = if (cooldownNanos > 0L) {
                nanoTime() + cooldownNanos
            } else {
                0L
            }
        }
    }

    private fun resetCooldownAfterQuietPeriod(config: ReconnectionSpanConfig, nowNanos: Long) {
        val lastThrottledAtNanos = lastThrottledAtNanos ?: return
        val resetNanos = config.cooldownResetMillis.toNanosAtLeastZero()
        if (nowNanos - cooldownUntilNanos >= 0L
            && resetNanos > 0L
            && nowNanos - lastThrottledAtNanos >= resetNanos
        ) {
            cooldownLevel = 0
            this.lastThrottledAtNanos = null
        }
    }

    private fun isInsideCooldown(nowNanos: Long): Boolean {
        return hasEmittedSpan && nowNanos - cooldownUntilNanos < 0L
    }

    private fun throttleAndIncreaseCooldown(config: ReconnectionSpanConfig, nowNanos: Long) {
        incrementThrottledConnectionDropCount()
        lastThrottledAtNanos = nowNanos

        if (config.baseCooldownMillis <= 0L) return

        cooldownLevel = (cooldownLevel + 1).coerceAtMost(MAX_COOLDOWN_LEVEL)
        cooldownUntilNanos = nowNanos + currentCooldownNanos(config)
    }

    private fun currentCooldownNanos(config: ReconnectionSpanConfig): Long {
        val baseCooldownNanos = config.baseCooldownMillis.toNanosAtLeastZero()
        if (baseCooldownNanos <= 0L) return 0L

        val multiplier = 1L shl (cooldownLevel - 1).coerceAtLeast(0)
        val maxCooldownNanos = config.maxCooldownMillis.toNanosAtLeastZero()
        val cooldownNanos = baseCooldownNanos.saturatingMultiply(multiplier)
        return if (maxCooldownNanos > 0L) {
            cooldownNanos.coerceAtMost(maxCooldownNanos)
        } else {
            cooldownNanos
        }
    }

    private fun incrementThrottledConnectionDropCount() {
        if (throttledConnectionDropCount < Long.MAX_VALUE) {
            throttledConnectionDropCount++
        }
    }

    private fun Long.toNanosAtLeastZero(): Long {
        return TimeUnit.MILLISECONDS.toNanos(coerceAtLeast(0L))
    }

    private fun Long.saturatingMultiply(multiplier: Long): Long {
        if (this == 0L || multiplier == 0L) return 0L
        if (this > Long.MAX_VALUE / multiplier) return Long.MAX_VALUE
        return this * multiplier
    }

    private companion object {
        const val MAX_COOLDOWN_LEVEL = 30
    }
}
