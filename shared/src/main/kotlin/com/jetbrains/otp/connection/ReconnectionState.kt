package com.jetbrains.otp.connection

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.exporter.FrequentPerformanceMetricsReporter
import com.jetbrains.otp.span.DefaultRootSpanService
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode

class ReconnectionState internal constructor(
    private val config: ReconnectionSpanConfig,
    private val telemetry: ReconnectionTelemetry,
    private val limiter: ReconnectionSpanLimiter,
) {
    constructor(config: ReconnectionSpanConfig) : this(
        config = config,
        telemetry = DefaultReconnectionTelemetry,
        limiter = GlobalReconnectionSpanLimiter,
    )

    private var connected: Boolean? = null
    private var reconnectionSpan: ReconnectionSpan? = null

    @Synchronized
    fun disconnected(context: Map<String, String> = emptyMap()) {
        if (connected == false) return
        connected = false

        val permit = limiter.tryAcquire(config) ?: return

        telemetry.reportMetricsAfterConnectionDrop()

        reconnectionSpan?.end()
        reconnectionSpan = telemetry.startSpan(
            spanName = config.spanName,
            context = context,
            throttledConnectionDropCount = permit.throttledConnectionDropCount,
        )
    }

    @Synchronized
    fun expectedDisconnected(
        reason: ExpectedDisconnectReason,
        context: Map<String, String> = emptyMap(),
    ) {
        if (connected == false && reconnectionSpan == null) return
        connected = false

        reconnectionSpan?.apply {
            setAttributes(context + (EXPECTED_DISCONNECT_REASON_ATTRIBUTE to reason.attributeValue))
            end()
            limiter.onSpanEnded(config)
        }
        reconnectionSpan = null
    }

    @Synchronized
    fun connected(context: Map<String, String> = emptyMap()) {
        if (connected == true) return
        connected = true

        reconnectionSpan?.apply {
            setAttributes(context)
            end()
            limiter.onSpanEnded(config)
        }
        reconnectionSpan = null
    }
}

internal interface ReconnectionSpanLimiter {
    fun tryAcquire(config: ReconnectionSpanConfig): ReconnectionSpanPermit?

    fun onSpanEnded(config: ReconnectionSpanConfig)
}

internal data class ReconnectionSpanPermit(
    val throttledConnectionDropCount: Long,
)

private val GlobalReconnectionSpanLimiter = AdaptiveCooldownReconnectionSpanLimiter()
private val LOG = Logger.getInstance(ReconnectionState::class.java)
private const val EXPECTED_DISCONNECT_REASON_ATTRIBUTE = "reconnection.expected.reason"

internal interface ReconnectionTelemetry {
    fun reportMetricsAfterConnectionDrop()

    fun startSpan(
        spanName: String,
        context: Map<String, String>,
        throttledConnectionDropCount: Long,
    ): ReconnectionSpan
}

internal interface ReconnectionSpan {
    fun setAttributes(context: Map<String, String>)

    fun end()
}

private object DefaultReconnectionTelemetry : ReconnectionTelemetry {
    override fun reportMetricsAfterConnectionDrop() {
        runCatching {
            FrequentPerformanceMetricsReporter.getInstance().reportMetricsPrecisely()
        }.onFailure { error ->
            LOG.warn("Failed to report frequent performance metrics after connection drop", error)
        }
    }

    override fun startSpan(
        spanName: String,
        context: Map<String, String>,
        throttledConnectionDropCount: Long,
    ): ReconnectionSpan {
        val span = DefaultRootSpanService.TRACER
            .spanBuilder(spanName)
            .startSpan()
            .apply {
                setStatus(StatusCode.ERROR)
                setAttributes(context)
                if (throttledConnectionDropCount > 0L) {
                    setAttribute(THROTTLED_CONNECTION_DROP_COUNT_ATTRIBUTE, throttledConnectionDropCount)
                }
                setMainThreadStatusAttribute()
            }

        return OpenTelemetryReconnectionSpan(span)
    }

    private fun Span.setAttributes(context: Map<String, String>) {
        context.forEach { (attributeName, value) ->
            setAttribute(AttributeKey.stringKey(attributeName), value)
        }
    }

    private fun Span.setMainThreadStatusAttribute() {
        val (isModalDialogOpened, isMainThreadStatusCheckTimedOut) = checkMainThreadStatus()
        if (isMainThreadStatusCheckTimedOut) return

        setAttribute(MODAL_DIALOG_OPENED_ATTRIBUTE, isModalDialogOpened)
    }

    private const val MODAL_DIALOG_OPENED_ATTRIBUTE = "modal.dialog.opened"
    private const val THROTTLED_CONNECTION_DROP_COUNT_ATTRIBUTE = "reconnection.throttled.drop_count"
}

private class OpenTelemetryReconnectionSpan(
    private val span: Span,
) : ReconnectionSpan {
    override fun setAttributes(context: Map<String, String>) {
        context.forEach { (attributeName, value) ->
            span.setAttribute(AttributeKey.stringKey(attributeName), value)
        }
    }

    override fun end() {
        span.end()
    }
}

data class ReconnectionSpanConfig(
    val spanName: String,
    val baseCooldownMillis: Long = DEFAULT_BASE_COOLDOWN_MILLIS,
    val maxCooldownMillis: Long = DEFAULT_MAX_COOLDOWN_MILLIS,
    val cooldownResetMillis: Long = DEFAULT_COOLDOWN_RESET_MILLIS,
) {
    companion object {
        const val DEFAULT_BASE_COOLDOWN_MILLIS = 6 * 60 * 1000L
        const val DEFAULT_MAX_COOLDOWN_MILLIS = 60 * 60 * 1000L
        const val DEFAULT_COOLDOWN_RESET_MILLIS = 60 * 60 * 1000L
    }
}
