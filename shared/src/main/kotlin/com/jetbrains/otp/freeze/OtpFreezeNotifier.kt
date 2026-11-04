package com.jetbrains.otp.freeze

import com.intellij.diagnostic.FreezeNotifier
import com.intellij.diagnostic.LogMessage
import com.intellij.diagnostic.ThreadDump
import com.intellij.openapi.extensions.PluginId
import java.time.Instant

/** Uses the platform's analyzed report when auto-reporting is disabled. */
@Suppress("UnstableApiUsage")
class OtpFreezeNotifier : FreezeNotifier {
    override suspend fun notifyFreeze(
        event: LogMessage,
        problematicPluginId: PluginId,
        currentDumps: Collection<ThreadDump>,
        durationMs: Long,
    ) {
        val stackTrace = event.throwable.stackTraceToString()
        val dumps = currentDumps.toList()
        val endTime = Instant.now()
        FreezeReportingService.getInstance().report(reportDir = null, fromListener = false) {
            OtpFreezeReporter.reportFreeze(stackTrace, dumps, durationMs, endTime)
        }
    }
}
