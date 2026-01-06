package com.jetbrains.otp.span

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope


@Suppress("UnstableApiUsage")
@Service(Service.Level.APP)
class DefaultRootSpanService : AppLifecycleListener {
    private val tracer = GlobalOpenTelemetry.get().getTracer("com.jetbrains.otp.diagnostic")

    @Volatile
    private var currentSpan: Span? = null

    @Volatile
    private var currentScope: Scope? = null

    @Volatile
    private var defaultSpan: Span? = null

    @Volatile
    private var defaultScope: Scope? = null

    init {
        ApplicationManager.getApplication().messageBus.simpleConnect().subscribe(AppLifecycleListener.TOPIC, this)
    }

    @Synchronized
    fun startSessionSpan(sessionId: String): Span {
        defaultScope?.close()
        defaultSpan?.end()
        defaultSpan = null
        defaultScope = null

        currentScope?.close()
        currentSpan?.end()

        currentSpan = tracer.spanBuilder("remote-dev-session")
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
        currentSpan?.end()
        currentSpan = null
        currentScope = null
    }

    @Synchronized
    private fun getOrCreateDefaultSpan(): Span {
        if (defaultSpan == null) {
            defaultSpan = tracer.spanBuilder("application-init")
                .startSpan()
            defaultScope = defaultSpan!!.makeCurrent()
        }
        return defaultSpan!!
    }

    override fun appWillBeClosed(isRestart: Boolean) {
        currentScope?.close()
        currentSpan?.end()
        defaultScope?.close()
        defaultSpan?.end()
    }

    companion object {
        fun getInstance(): DefaultRootSpanService = service()

        fun currentSpan(): Span {
            val instance = getInstance()
            instance.currentSpan?.let { return it }
            val contextSpan = Span.current()
            if (contextSpan.spanContext.isValid) {
                return contextSpan
            }
            return instance.getOrCreateDefaultSpan()
        }
    }
}