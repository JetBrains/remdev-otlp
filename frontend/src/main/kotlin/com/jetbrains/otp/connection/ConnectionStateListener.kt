package com.jetbrains.otp.connection

import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.otp.exporter.CpuUsageWindowMetricsReporter
import com.jetbrains.otp.span.DefaultRootSpanService
import com.jetbrains.thinclient.diagnostics.ThinClientConnectionState
import com.jetbrains.thinclient.diagnostics.ThinClientDiagnosticsService
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode

class ConnectionStateListener : ProjectActivity {
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
                        CpuUsageWindowMetricsReporter.getInstance().reportMetricsPrecisely()
                    }.onFailure { error ->
                        LOG.warn("Failed to report CPU window metrics after connection drop", error)
                    }

                    reconnectionSpan?.end()
                    reconnectionSpan = tracer.spanBuilder("connection-dropped-reconnecting")
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
