package com.jetbrains.otp.connection

import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.exporter.FrequentPerformanceMetricsReporter
import com.jetbrains.otp.span.DefaultRootSpanService
import com.jetbrains.thinclient.diagnostics.ThinClientConnectionState
import com.jetbrains.thinclient.diagnostics.ThinClientDiagnosticsService
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import com.jetbrains.otp.freeze.StackTraceAbbreviator
import java.io.PrintWriter
import java.io.StringWriter

class ConnectionStateListener : ProjectActivity {
    private val stackTraceAbbreviator = StackTraceAbbreviator()

    override suspend fun execute(project: Project) {
        var connected: Boolean? = null
        var reconnectionSpan: Span? = null
        val tracer = DefaultRootSpanService.TRACER

        ThinClientDiagnosticsService.getInstance(project).connectionState.advise(project.lifetime) {
            when (it) {
                is ThinClientConnectionState.WireNotConnected, is ThinClientConnectionState.NoUiThreadPing -> {
                    if (connected == false) return@advise
                    connected = false

                    runCatching {
                        FrequentPerformanceMetricsReporter.getInstance().reportMetricsPrecisely()
                    }.onFailure { error ->
                        LOG.warn("Failed to report frequent performance metrics after connection drop", error)
                    }

                    // Capture current stacktrace for debugging
                    val stackTrace = Exception("Connection dropped - capturing stack for diagnostics").let { ex ->
                        val sw = StringWriter()
                        ex.printStackTrace(PrintWriter(sw))
                        stackTraceAbbreviator.abbreviateStackTraces(sw.toString())
                    }

                    reconnectionSpan?.end()
                    reconnectionSpan = tracer.spanBuilder("connection-dropped-reconnecting")
                        .setAllAttributes(
                            Attributes.of(
                                AttributeKey.stringKey("stackTrace"), stackTrace
                            )
                        )
                        .startSpan()
                    reconnectionSpan?.setStatus(StatusCode.ERROR)
                }
                is ThinClientConnectionState.Connected -> {
                    if (connected == true) return@advise
                    connected = true

                    reconnectionSpan?.end()
                    reconnectionSpan = null
                }
                else -> {}
            }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(ConnectionStateListener::class.java)
    }
}
