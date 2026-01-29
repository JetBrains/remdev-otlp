package com.jetbrains.otp.freeze

import com.intellij.diagnostic.FreezeNotifier
import com.intellij.diagnostic.LogMessage
import com.intellij.diagnostic.ThreadDump
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import java.time.Instant

@Suppress("UnstableApiUsage")
class OtpFreezeNotifier : FreezeNotifier {
    private val tracer = GlobalOpenTelemetry.get().getTracer("com.jetbrains.otp.diagnostic")
    private val stackTraceAbbreviator = StackTraceAbbreviator()

    companion object {
        private const val MAX_THREAD_DUMP_SIZE_BYTES = 64 * 1024
    }

    override fun notifyFreeze(
        event: LogMessage,
        currentDumps: Collection<ThreadDump>,
        reportDir: Path,
        durationMs: Long
    ) {
        val endTime = Instant.now()
        val startTime = endTime.minusMillis(durationMs)
        val span = tracer.spanBuilder("ui-thread-freeze")
            .setStartTimestamp(startTime)
            .setAllAttributes(
                Attributes.of(
                    AttributeKey.stringKey("stackTrace"), getAbbreviatedStackTrace(event.throwable)
                )
            ).startSpan()

        span.setStatus(StatusCode.ERROR)

        currentDumps.take(4).forEachIndexed { index, dump ->
            val abbreviatedDump = stackTraceAbbreviator.abbreviateStackTraces(dump.rawDump)
            val truncatedDump = stackTraceAbbreviator.truncateToMaxBytes(abbreviatedDump, MAX_THREAD_DUMP_SIZE_BYTES)
            span.setAttribute("thread_dump_$index", truncatedDump)
        }

        span.end(endTime)
    }

    fun getAbbreviatedStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        return stackTraceAbbreviator.abbreviateStackTraces(sw.toString())
    }
}