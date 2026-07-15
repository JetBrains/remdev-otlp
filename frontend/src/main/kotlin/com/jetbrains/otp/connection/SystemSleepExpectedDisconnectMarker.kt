package com.jetbrains.otp.connection

import com.jetbrains.otp.api.ExpectedDisconnectApi
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.awt.desktop.SystemSleepEvent
import java.awt.desktop.SystemSleepListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class SystemSleepExpectedDisconnectMarker(
    private val expectedDisconnectTracker: ExpectedDisconnectTracker,
    private val coroutineScope: CoroutineScope,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : SystemSleepListener {
    private var desktop: Desktop? = null
    private var heartbeatJob: Job? = null
    private var lastObservedMillis: Long = currentTimeMillis()

    fun install() {
        installDesktopListener()
        heartbeatJob = coroutineScope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MILLIS)
                markIfSystemSleepDetected()
            }
        }
    }

    fun dispose() {
        heartbeatJob?.cancel()
        desktop?.let {
            runCatching { it.removeAppEventListener(this) }
        }
        desktop = null
    }

    override fun systemAboutToSleep(event: SystemSleepEvent) {
        markLocalExpectedDisconnect(ExpectedDisconnectTracker.SYSTEM_SLEEP_TTL_MILLIS)
        markBackendExpectedDisconnect(ExpectedDisconnectTracker.REMOTE_SYSTEM_SLEEP_TTL_MILLIS)
    }

    override fun systemAwoke(event: SystemSleepEvent) {
        markLocalExpectedDisconnect(ExpectedDisconnectTracker.SYSTEM_WAKE_TTL_MILLIS)
        updateLastObservedMillis()
    }

    @Synchronized
    fun markIfSystemSleepDetected() {
        val nowMillis = currentTimeMillis()
        val elapsedMillis = nowMillis - lastObservedMillis
        lastObservedMillis = nowMillis

        if (elapsedMillis >= SYSTEM_SLEEP_GAP_MILLIS) {
            markLocalExpectedDisconnect(ExpectedDisconnectTracker.SYSTEM_WAKE_TTL_MILLIS)
        }
    }

    private fun installDesktopListener() {
        val desktop = desktop() ?: return
        if (!desktop.isSupported(Desktop.Action.APP_EVENT_SYSTEM_SLEEP)) return
        runCatching {
            desktop.addAppEventListener(this)
            this.desktop = desktop
        }
    }

    private fun desktop(): Desktop? {
        if (GraphicsEnvironment.isHeadless()) return null
        if (!Desktop.isDesktopSupported()) return null
        return Desktop.getDesktop()
    }

    @Synchronized
    private fun updateLastObservedMillis() {
        lastObservedMillis = currentTimeMillis()
    }

    private fun markLocalExpectedDisconnect(ttlMillis: Long) {
        expectedDisconnectTracker.mark(ExpectedDisconnectReason.SYSTEM_SLEEP, ttlMillis)
    }

    private fun markBackendExpectedDisconnect(ttlMillis: Long) {
        coroutineScope.launch {
            runCatching {
                ExpectedDisconnectApi.getInstance().markExpectedDisconnect(
                    reasonName = ExpectedDisconnectReason.SYSTEM_SLEEP.name,
                    ttlMillis = ttlMillis,
                )
            }
        }
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MILLIS = 5_000L
        const val SYSTEM_SLEEP_GAP_MILLIS = 30_000L
    }
}
