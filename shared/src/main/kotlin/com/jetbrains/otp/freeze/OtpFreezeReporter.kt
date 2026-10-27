package com.jetbrains.otp.freeze

import com.intellij.diagnostic.ThreadDump
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.exporter.FrequentPerformanceMetricsReporter
import com.jetbrains.otp.span.DefaultRootSpanService
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import java.time.Instant
import kotlin.math.roundToInt

internal object OtpFreezeReporter {
    private const val MAX_THREAD_DUMP_SIZE_BYTES = 64 * 1024
    private val LOG = Logger.getInstance(OtpFreezeReporter::class.java)
    private val stackTraceAbbreviator = StackTraceAbbreviator()

    fun reportFreeze(stackTrace: String, dumps: Collection<ThreadDump>, durationMs: Long, endTime: Instant = Instant.now()) {
        runCatching {
            FrequentPerformanceMetricsReporter.getInstance().reportMetricsPrecisely()
        }.onFailure { error ->
            LOG.warn("Failed to report frequent performance metrics after freeze", error)
        }

        val tracer = GlobalOpenTelemetry.get().getTracer(DefaultRootSpanService.DIAGNOSTIC_TRACER_NAME)
        val span = tracer.spanBuilder("ui-thread-freeze")
            .setStartTimestamp(endTime.minusMillis(durationMs))
            .setAllAttributes(
                Attributes.of(
                    AttributeKey.stringKey("stackTrace"), stackTraceAbbreviator.abbreviateStackTraces(stackTrace),
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
