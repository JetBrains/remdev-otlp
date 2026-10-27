package com.jetbrains.otp.freeze

import com.intellij.diagnostic.PerformanceListener
import com.intellij.diagnostic.ThreadDump
import com.intellij.internal.DebugAttachDetector
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path
import java.time.Instant

/** Reports freezes when auto-reporting can bypass `FreezeNotifier`. */
@Suppress("UnstableApiUsage")
class OtpFreezePerformanceListener : PerformanceListener {
    private val lock = Any()
    private var freezeInProgress = false
    private val currentDumps = ArrayList<ThreadDump>()
    private var commonEdtStack: List<StackTraceElement>? = null
    private var maxDumpDurationMs = 0
    private var freezeEndTime: Instant? = null

    companion object {
        private const val MIN_DUMPS = 2

        /** Applies report eligibility checks to the dumps exposed by the performance watcher. */
        internal fun shouldReport(
            durationMs: Long,
            settings: FreezeReporterSettings,
            dumpCount: Int,
            commonEdtStack: List<StackTraceElement>?,
        ): Boolean {
            return settings.enabled
                   && (durationMs / 1000) > settings.durationThresholdSeconds.coerceAtLeast(0)
                   && settings.maxDumpDurationMs > 0
                   && dumpCount >= MIN_DUMPS
                   && !commonEdtStack.isNullOrEmpty()
                   && commonEdtStack.none {
                       it.className == "com.intellij.openapi.application.impl.ApplicationImpl"
                       && it.methodName == "runEdtProgressWriteAction"
                   }
        }

        /** Frames shared by all EDT dumps, counted from the bottom of the stack. */
        internal fun commonStackSuffix(
            commonPart: List<StackTraceElement>?,
            edtStack: Array<StackTraceElement>,
        ): List<StackTraceElement> {
            if (commonPart == null) return edtStack.toList()

            var matching = 0
            while (matching < commonPart.size && matching < edtStack.size
                   && sameFrame(commonPart[commonPart.size - matching - 1], edtStack[edtStack.size - matching - 1])) {
                matching++
            }
            return commonPart.subList(commonPart.size - matching, commonPart.size)
        }

        private fun sameFrame(first: StackTraceElement, second: StackTraceElement): Boolean =
            first.className == second.className && first.methodName == second.methodName && first.fileName == second.fileName
    }

    override fun uiFreezeStarted(reportDir: Path, coroutineScope: CoroutineScope) {
        synchronized(lock) {
            reset()
            val settings = FreezeReporterSettings.read()
            maxDumpDurationMs = settings.maxDumpDurationMs
            freezeInProgress = !DebugAttachDetector.isAttached() && maxDumpDurationMs > 0
        }
    }

    override fun dumpedThreads(toFile: Path, dump: ThreadDump) {
        synchronized(lock) {
            if (!freezeInProgress) return
            currentDumps.add(dump)
            dump.edtStackTrace?.let { edtStack ->
                commonEdtStack = commonStackSuffix(commonEdtStack, edtStack)
            }
        }
    }

    override fun uiFreezeFinished(durationMs: Long, reportDir: Path?) {
        synchronized(lock) {
            if (freezeInProgress) {
                freezeEndTime = Instant.now()
            }
        }
    }

    override fun uiFreezeRecorded(durationMs: Long, reportDir: Path?) {
        val dumps: List<ThreadDump>
        val commonStack: List<StackTraceElement>
        val endTime: Instant
        synchronized(lock) {
            if (!freezeInProgress) return

            val settings = FreezeReporterSettings.read().copy(maxDumpDurationMs = maxDumpDurationMs)
            val report = shouldReport(
                durationMs = durationMs,
                settings = settings,
                dumpCount = currentDumps.size,
                commonEdtStack = commonEdtStack,
            )
            dumps = ArrayList(currentDumps)
            commonStack = commonEdtStack.orEmpty()
            endTime = freezeEndTime ?: Instant.now()
            reset()
            if (!report) return
        }

        val stackTrace = buildString {
            append("com.intellij.diagnostic.Freeze")
            commonStack.forEach { frame -> append("\n\tat ").append(frame) }
            append('\n')
        }
        FreezeReportingService.getInstance().report(reportDir, fromListener = true) {
            OtpFreezeReporter.reportFreeze(stackTrace, dumps, durationMs, endTime)
        }
    }

    private fun reset() {
        freezeInProgress = false
        currentDumps.clear()
        commonEdtStack = null
        maxDumpDurationMs = 0
        freezeEndTime = null
    }
}
