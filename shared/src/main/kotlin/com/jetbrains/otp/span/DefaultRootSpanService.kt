package com.jetbrains.otp.span

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.otp.exporter.TelemetrySpanExporter
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Scope
import io.opentelemetry.sdk.trace.ReadableSpan


@Suppress("UnstableApiUsage")
@Service(Service.Level.APP)
class DefaultRootSpanService : AppLifecycleListener {

    @Volatile
    private var currentSpan: Span? = null

    @Volatile
    private var currentScope: Scope? = null

    @Volatile
    private var defaultSpan: Span? = null

    init {
        ApplicationManager.getApplication().messageBus.connect().subscribe(AppLifecycleListener.TOPIC, this)
    }

    @Synchronized
    fun startSessionSpan(sessionId: String): Span {
        defaultSpan?.end()
        defaultSpan = null

        currentScope?.close()
        endAndExport(currentSpan, wait = false)

        currentSpan = TRACER.spanBuilder("remote-dev-session")
            .setNoParent()
            .setAllAttributes(
                Attributes.of(
                    AttributeKey.stringKey("session.id"), sessionId
                )
            )
            .startSpan()

        currentScope = currentSpan!!.makeCurrent()

        return currentSpan!!
    }

    @Synchronized
    fun endSessionSpan() {
        currentScope?.close()
        endAndExport(currentSpan, wait = false)
        currentSpan = null
        currentScope = null
    }

    @Synchronized
    fun adoptRemoteSessionSpan(spanId: String, traceId: String) {
        defaultSpan?.end()
        defaultSpan = null

        currentScope?.close()
        currentScope = null
        currentSpan?.end()
        currentSpan = Span.wrap(
            SpanContext.create(
                traceId,
                spanId,
                TraceFlags.getSampled(),
                TraceState.getDefault()
            )
        )
    }

    @Synchronized
    private fun getOrCreateDefaultSpan(): Span {
        if (defaultSpan == null) {
            defaultSpan = TRACER.spanBuilder("application-init")
                .startSpan()
        }
        return defaultSpan!!
    }

    override fun appWillBeClosed(isRestart: Boolean) {
        currentScope?.close()
        // The application can still send here; the platform would only flush the root during late shutdown.
        endAndExport(currentSpan, wait = true)
        defaultSpan?.end()
    }

    /** Ends [span] and exports it right away instead of leaving it to the platform's span batch. */
    private fun endAndExport(span: Span?, wait: Boolean) {
        span ?: return
        span.end()
        // Only spans started here carry data; the backend adopts the frontend session as a bare span context.
        val spanData = (span as? ReadableSpan)?.toSpanData() ?: return
        TelemetrySpanExporter.getInstance().exportEndedSpan(spanData, wait)
    }

    companion object {
        fun getInstance(): DefaultRootSpanService = service()

        fun currentSpan(): Span {
            val currentSpan = Span.current()
            if(currentSpan.spanContext.isValid) return currentSpan
            val instance = getInstance()
            instance.currentSpan?.let { return it }
            return instance.getOrCreateDefaultSpan()
        }

        const val DIAGNOSTIC_TRACER_NAME = "com.jetbrains.otp.diagnostic"

        val TRACER = GlobalOpenTelemetry.get().getTracer(DIAGNOSTIC_TRACER_NAME)
    }
}
