package com.jetbrains.otp.connection

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.exporter.FrequentPerformanceMetricsReporter
import com.jetbrains.otp.span.DefaultRootSpanService
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode

class ReconnectionState(
    private val config: ReconnectionSpanConfig,
) {
    private var connected: Boolean? = null
    private var reconnectionSpan: Span? = null

    @Synchronized
    fun disconnected(context: Map<String, String> = emptyMap()) {
        if (connected == false) return
        connected = false

        runCatching {
            FrequentPerformanceMetricsReporter.getInstance().reportMetricsPrecisely()
        }.onFailure { error ->
            LOG.warn("Failed to report frequent performance metrics after connection drop", error)
        }

        reconnectionSpan?.end()
        reconnectionSpan = DefaultRootSpanService.TRACER
            .spanBuilder(config.spanName)
            .startSpan()
            .apply {
                setStatus(StatusCode.ERROR)
                setAttributes(context)
                setMainThreadStatusAttribute()
            }
    }

    @Synchronized
    fun connected(context: Map<String, String> = emptyMap()) {
        if (connected == true) return
        connected = true

        reconnectionSpan?.apply {
            setAttributes(context)
            end()
        }
        reconnectionSpan = null
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

    private companion object {
        val LOG = Logger.getInstance(ReconnectionState::class.java)

        const val MODAL_DIALOG_OPENED_ATTRIBUTE = "modal.dialog.opened"
    }
}

data class ReconnectionSpanConfig(
    val spanName: String,
)
