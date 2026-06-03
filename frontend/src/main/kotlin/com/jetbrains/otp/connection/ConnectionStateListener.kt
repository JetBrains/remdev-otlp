package com.jetbrains.otp.connection

import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.frontend.split.connection.diagnostics.ThinClientConnectionState
import com.jetbrains.thinclient.diagnostics.ThinClientDiagnosticsService

class ConnectionStateListener : ProjectActivity {
    override suspend fun execute(project: Project) {
        val reconnectionState = ReconnectionState(
            ReconnectionSpanConfig(spanName = RECONNECTION_SPAN_NAME)
        )

        ThinClientDiagnosticsService.getInstance(project).connectionState.advise(project.lifetime) {
            when (it) {
                is ThinClientConnectionState.WireNotConnected, is ThinClientConnectionState.NoUiThreadPing -> {
                    reconnectionState.disconnected()
                }
                is ThinClientConnectionState.Connected -> {
                    reconnectionState.connected()
                }
                else -> {}
            }
        }
    }

    companion object {
        private const val RECONNECTION_SPAN_NAME = "connection-dropped-reconnecting"
    }
}
