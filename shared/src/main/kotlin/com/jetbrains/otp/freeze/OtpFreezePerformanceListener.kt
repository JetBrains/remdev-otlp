package com.jetbrains.otp.freeze

import com.intellij.diagnostic.PerformanceListener
import com.intellij.diagnostic.ThreadDump
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.otp.exporter.FrequentPerformanceMetricsReporter
import com.jetbrains.otp.span.DefaultRootSpanService
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path
import java.time.Instant
import kotlin.math.roundToInt

/** Reports UI freezes, including ones the platform auto-reports and does not pass to `FreezeNotifier`. */
@Suppress("UnstableApiUsage")
class OtpFreezePerformanceListener : PerformanceListener {
    private val tracer by lazy { GlobalOpenTelemetry.get().getTracer(DefaultRootSpanService.DIAGNOSTIC_TRACER_NAME) }
    private val stackTraceAbbreviator = StackTraceAbbreviator()

    private val lock = Any()
    private var freezeInProgress = false
    private val currentDumps = ArrayList<ThreadDump>()
    private var commonEdtStack: List<StackTraceElement>? = null

    companion object {
        private const val MAX_THREAD_DUMP_SIZE_BYTES = 64 * 1024
        private const val MIN_DUMPS = 2
        private const val REPORTER_ENABLED_KEY = "freeze.reporter.enabled"
        private const val DURATION_THRESHOLD_KEY = "freeze.reporter.duration.threshold.seconds"
        private const val DEFAULT_DURATION_THRESHOLD_SECONDS = 10
        private val LOG = Logger.getInstance(OtpFreezePerformanceListener::class.java)

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

        reportFreeze(commonStack, dumps, durationMs)
    }

    private fun reportFreeze(commonStack: List<StackTraceElement>, dumps: List<ThreadDump>, durationMs: Long) {
        runCatching {
            FrequentPerformanceMetricsReporter.getInstance().reportMetricsPrecisely()
        }.onFailure { error ->
            LOG.warn("Failed to report frequent performance metrics after freeze", error)
        }

        val endTime = Instant.now()
        val startTime = endTime.minusMillis(durationMs)
        val span = tracer.spanBuilder("ui-thread-freeze")
            .setStartTimestamp(startTime)
            .setAllAttributes(
                Attributes.of(
                    AttributeKey.stringKey("stackTrace"), getAbbreviatedStackTrace(commonStack)
                )
            ).startSpan()

        span.setStatus(StatusCode.ERROR)

        selectFreezeDumps(dumps).forEachIndexed { index, dump ->
            val abbreviatedDump = stackTraceAbbreviator.abbreviateStackTraces(dump.rawDump)
            val truncatedDump = stackTraceAbbreviator.truncateToMaxBytes(abbreviatedDump, MAX_THREAD_DUMP_SIZE_BYTES)
            span.setAttribute("thread_dump_$index", truncatedDump)
        }

        span.end(endTime)
    }

    private fun getAbbreviatedStackTrace(frames: List<StackTraceElement>): String {
        val stackTrace = buildString {
            append("com.intellij.diagnostic.Freeze")
            frames.forEach { frame -> append("\n\tat ").append(frame) }
            append('\n')
        }
        return stackTraceAbbreviator.abbreviateStackTraces(stackTrace)
    }

    private fun reset() {
        freezeInProgress = false
        currentDumps.clear()
        commonEdtStack = null
    }
}

internal fun selectFreezeDumps(dumps: Collection<ThreadDump>): List<ThreadDump> {
    val maxDumps = 4
    val orderedDumps = dumps.toList()
    if (orderedDumps.size <= maxDumps) return orderedDumps

    // The platform supplies dumps in capture order; include both ends of the freeze.
    return List(maxDumps) { index ->
        orderedDumps[(index * orderedDumps.lastIndex.toDouble() / (maxDumps - 1)).roundToInt()]
    }
}
