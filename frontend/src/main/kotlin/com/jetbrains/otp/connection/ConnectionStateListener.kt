package com.jetbrains.otp.connection

import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.thinclient.PlatformFrontendSplitBundle
import com.jetbrains.thinclient.diagnostics.ThinClientConnectionState
import com.jetbrains.thinclient.diagnostics.ThinClientDiagnosticsService
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span

class ConnectionStateListener : ProjectActivity {
    override suspend fun execute(project: Project) {
        ThinClientDiagnosticsService.getInstance(project).connectionState.advise(project.lifetime) {
            when (it) {
                is ThinClientConnectionState.WireNotConnected, is ThinClientConnectionState.NoUiThreadPing -> {
                    // Record span event
                    Span.current().addEvent(
                        "connection.dropped",
                        Attributes.of(
                            AttributeKey.stringKey("project"), project.name
                        )
                    )
                }
                is ThinClientConnectionState.Connected -> {
                    Span.current().addEvent("connection.established")
                }
                else -> {}
            }
        }
    }
}