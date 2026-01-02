package com.jetbrains.otp.span

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope


@Service(Service.Level.APP)
class DefaultRootSpanService : Disposable {
    private val tracer = GlobalOpenTelemetry.get().getTracer("com.jetbrains.otp.diagnostic")

    @Volatile
    private var currentSpan: Span

    @Volatile
    private var currentScope: Scope

    init {
        currentSpan = createApplicationSpan()
        currentScope = currentSpan.makeCurrent()
    }

    @Synchronized
    fun startSessionSpan(sessionId: String): Span {
        currentScope.close()
        currentSpan.end()

        currentSpan = tracer.spanBuilder("remote-dev-session")
            .setAllAttributes(
                Attributes.of(
                    AttributeKey.stringKey("session.id"), sessionId
                )
            )
            .startSpan()

        currentScope = currentSpan.makeCurrent()

        return currentSpan
    }

    @Synchronized
    fun endSessionSpan() {
        currentScope.close()
        currentSpan.end()
        currentSpan = createApplicationSpan()
        currentScope = currentSpan.makeCurrent()
    }

    private fun createApplicationSpan(): Span {
        return tracer.spanBuilder("application-session")
            .setAllAttributes(
                Attributes.of(
                    AttributeKey.stringKey("service.name"), "otp-diagnostic"
                )
            )
            .startSpan()
    }

    override fun dispose() {
        currentScope.close()
        currentSpan.end()
    }

    companion object {
        fun getInstance(): DefaultRootSpanService = service()
        fun currentSpan(): Span = getInstance().currentSpan
    }
}