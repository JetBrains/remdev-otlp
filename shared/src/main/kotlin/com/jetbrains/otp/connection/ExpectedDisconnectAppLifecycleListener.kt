package com.jetbrains.otp.connection

import com.intellij.ide.AppLifecycleListener

class ExpectedDisconnectAppLifecycleListener : AppLifecycleListener {
    override fun appClosing() {
        markAppClosing()
    }

    override fun appWillBeClosed(isRestart: Boolean) {
        markAppClosing()
    }

    private fun markAppClosing() {
        ExpectedDisconnectTracker.getInstance().mark(
            ExpectedDisconnectReason.APP_CLOSING,
            ttlMillis = ExpectedDisconnectTracker.SHUTDOWN_TTL_MILLIS,
        )
    }
}
