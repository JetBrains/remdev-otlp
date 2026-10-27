package com.jetbrains.otp.freeze

import com.intellij.diagnostic.FreezeNotifier
import com.intellij.diagnostic.LogMessage
import com.intellij.diagnostic.ThreadDump
import java.nio.file.Path
import java.time.Instant

/** Uses the platform's analyzed report when auto-reporting is disabled. */
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
        FreezeReportingService.getInstance().report(reportDir, fromListener = false) {
            OtpFreezeReporter.reportFreeze(stackTrace, dumps, durationMs, endTime)
        }
    }
}
