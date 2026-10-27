package com.jetbrains.otp.freeze

import com.intellij.diagnostic.PerformanceListener
import com.intellij.diagnostic.ThreadDump
import com.intellij.openapi.util.registry.Registry
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path

/** Reports freezes when auto-reporting can bypass `FreezeNotifier`. */
@Suppress("UnstableApiUsage")
class OtpFreezePerformanceListener : PerformanceListener {
    private val lock = Any()
    private var freezeInProgress = false
    private val currentDumps = ArrayList<ThreadDump>()
    private var commonEdtStack: List<StackTraceElement>? = null

    companion object {
        private const val MIN_DUMPS = 2
        private const val REPORTER_ENABLED_KEY = "freeze.reporter.enabled"
        private const val DURATION_THRESHOLD_KEY = "freeze.reporter.duration.threshold.seconds"
        private const val DEFAULT_DURATION_THRESHOLD_SECONDS = 10

        /** Same filters the platform applies before reporting a freeze. */
        internal fun shouldReport(
            durationMs: Long,
            durationThresholdSeconds: Int,
            dumpCount: Int,
            commonEdtStack: List<StackTraceElement>?,
        ): Boolean {
            return (durationMs / 1000).toInt() > durationThresholdSeconds
                   && dumpCount >= MIN_DUMPS
                   && !commonEdtStack.isNullOrEmpty()
        }

        /** Frames shared by all EDT dumps, counted from the bottom of the stack. */
        internal fun commonStackSuffix(
            commonPart: List<StackTraceElement>?,
            edtStack: Array<StackTraceElement>,
        ): List<StackTraceElement> {
            if (commonPart == null) return edtStack.toList()

            var matching = 0
            while (matching < commonPart.size && matching < edtStack.size
                   && commonPart[commonPart.size - matching - 1] == edtStack[edtStack.size - matching - 1]) {
                matching++
            }
            return commonPart.subList(commonPart.size - matching, commonPart.size)
        }
    }

    override fun uiFreezeStarted(reportDir: Path, coroutineScope: CoroutineScope) {
        synchronized(lock) {
            reset()
            freezeInProgress = true
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

    override fun uiFreezeRecorded(durationMs: Long, reportDir: Path?) {
        val dumps: List<ThreadDump>
        val commonStack: List<StackTraceElement>
        synchronized(lock) {
            val report = freezeInProgress && Registry.`is`(REPORTER_ENABLED_KEY, false) && shouldReport(
                durationMs = durationMs,
                durationThresholdSeconds = Registry.intValue(DURATION_THRESHOLD_KEY, DEFAULT_DURATION_THRESHOLD_SECONDS),
                dumpCount = currentDumps.size,
                commonEdtStack = commonEdtStack,
            )
            dumps = ArrayList(currentDumps)
            commonStack = commonEdtStack.orEmpty()
            reset()
            if (!report) return
        }

        val stackTrace = buildString {
            append("com.intellij.diagnostic.Freeze")
            commonStack.forEach { frame -> append("\n\tat ").append(frame) }
            append('\n')
        }
        FreezeReportingService.getInstance().report(reportDir, fromListener = true) {
            OtpFreezeReporter.reportFreeze(stackTrace, dumps, durationMs)
        }
    }

    private fun reset() {
        freezeInProgress = false
        currentDumps.clear()
        commonEdtStack = null
    }
}
