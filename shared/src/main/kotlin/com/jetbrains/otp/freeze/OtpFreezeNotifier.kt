package com.jetbrains.otp.freeze

import com.intellij.diagnostic.FreezeNotifier
import com.intellij.diagnostic.LogMessage
import com.intellij.diagnostic.ThreadDump
import java.nio.file.Path
import java.time.Instant

/** Exports the platform's analyzed freeze report. */
@Suppress("UnstableApiUsage")
class OtpFreezeNotifier : FreezeNotifier {
    override fun notifyFreeze(
        event: LogMessage,
        currentDumps: Collection<ThreadDump>,
        reportDir: Path,
        durationMs: Long,
    ) {
        val stackTrace = event.throwable.stackTraceToString()
        val dumps = currentDumps.toList()
        val endTime = Instant.now()
        OtpFreezeReporter.reportFreeze(stackTrace, dumps, durationMs, endTime)
    }
}
