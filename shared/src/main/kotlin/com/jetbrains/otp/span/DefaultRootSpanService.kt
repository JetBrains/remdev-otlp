package com.jetbrains.otp.span

import com.intellij.ide.AppLifecycleListener
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
        currentSpan?.end()

        currentSpan = TRACER.spanBuilder("remote-dev-session")
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
            defaultSpan = TRACER.spanBuilder("application-init")
                .startSpan()
        }
        return defaultSpan!!
    }

    override fun appWillBeClosed(isRestart: Boolean) {
        currentScope?.close()
        currentSpan?.end()
        defaultSpan?.end()
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

        val TRACER = GlobalOpenTelemetry.get().getTracer("com.jetbrains.otp.diagnostic")
    }
}