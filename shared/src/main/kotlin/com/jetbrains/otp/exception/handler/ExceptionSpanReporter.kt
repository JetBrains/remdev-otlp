package com.jetbrains.otp.exception.handler

import com.jetbrains.otp.freeze.StackTraceAbbreviator
import com.jetbrains.otp.span.DefaultRootSpanService
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import java.io.PrintWriter
import java.io.StringWriter

internal object ExceptionSpanReporter {
    private const val EXCEPTION_SPAN_NAME_PREFIX = "ide-exception"
    private const val EXCEPTION_SOURCE = "platform-log"
    private const val MAX_STACKTRACE_SIZE_BYTES = 64 * 1024

    private val stackTraceAbbreviator = StackTraceAbbreviator()

    fun report(
        exception: Throwable,
        context: Map<String, String?> = emptyMap(),
    ) {
        val parent = DefaultRootSpanService.currentSpan()
        val span = DefaultRootSpanService.TRACER.spanBuilder(spanNameFor(exception))
            .setParent(Context.current().with(parent))
            .setAllAttributes(createAttributes(exception, context))
            .startSpan()

        try {
            span.setStatus(StatusCode.ERROR, exception.message ?: exception.javaClass.name)
        } finally {
            span.end()
        }
    }

    internal fun spanNameFor(exception: Throwable): String {
        return "$EXCEPTION_SPAN_NAME_PREFIX.${exceptionClassName(exception)}"
    }

    private fun createAttributes(
        exception: Throwable,
        context: Map<String, String?>,
    ): Attributes {
        val builder = Attributes.builder()
            .put(AttributeKey.stringKey("exception.type"), exception.javaClass.name)
            .put(AttributeKey.stringKey("exception.source"), EXCEPTION_SOURCE)
            .put(AttributeKey.stringKey("exception.stacktrace"), getStackTrace(exception))

        exception.message?.let {
            builder.put(AttributeKey.stringKey("exception.message"), it)
        }

        context.forEach { (key, value) ->
            if (!value.isNullOrBlank()) {
                builder.put(AttributeKey.stringKey(key), value)
            }
        }

        return builder.build()
    }

    private fun getStackTrace(exception: Throwable): String {
        val writer = StringWriter()
        exception.printStackTrace(PrintWriter(writer))
        return stackTraceAbbreviator.truncateToMaxBytes(writer.toString(), MAX_STACKTRACE_SIZE_BYTES)
    }

    private fun exceptionClassName(exception: Throwable): String {
        var currentClass: Class<*>? = exception.javaClass
        while (currentClass != null && Throwable::class.java.isAssignableFrom(currentClass)) {
            val simpleName = currentClass.simpleName
            if (simpleName.isNotBlank()) {
                return simpleName
            }
            currentClass = currentClass.superclass
        }
        return exception.javaClass.name
    }
}
