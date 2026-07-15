package com.jetbrains.otp.connection

import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.split.connection.ConnectionDeclineReason
import com.jetbrains.rd.platform.codeWithMe.comms.state.ConnectionStateKind
import com.jetbrains.thinclient.connection.ConnectionCode
import com.jetbrains.thinclient.connection.state.ClientConnectionStateService
import com.jetbrains.thinclient.diagnostics.ThinClientConnectionState
import com.jetbrains.thinclient.diagnostics.ThinClientDiagnosticsService
import com.jetbrains.thinclient.link.LinkConnectionUtil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ConnectionStateListener : ProjectActivity {
    override suspend fun execute(project: Project) {
        val reconnectionState = ReconnectionState(
            ReconnectionSpanConfig(spanName = RECONNECTION_SPAN_NAME)
        )
        val expectedDisconnectTracker = ExpectedDisconnectTracker.getInstance()

        val systemSleepMarker = installExpectedDisconnectMarkers(project, expectedDisconnectTracker)

        ThinClientDiagnosticsService.getInstance(project).connectionState.advise(project.lifetime) {
            when (it) {
                is ThinClientConnectionState.WireNotConnected, is ThinClientConnectionState.NoUiThreadPing -> {
                    systemSleepMarker.markIfSystemSleepDetected()
                    val expectedReason = expectedDisconnectTracker.currentReason()
                    if (expectedReason == null) {
                        reconnectionState.disconnected()
                    } else {
                        reconnectionState.expectedDisconnected(expectedReason)
                    }
                }
                is ThinClientConnectionState.Connected -> {
                    reconnectionState.connected()
                }
                else -> {}
            }
        }
    }

    private fun installExpectedDisconnectMarkers(
        project: Project,
        expectedDisconnectTracker: ExpectedDisconnectTracker,
    ): SystemSleepExpectedDisconnectMarker {
        val clientConnectionState = ClientConnectionStateService.getInstance().state
        val systemSleepMarker = SystemSleepExpectedDisconnectMarker(
            expectedDisconnectTracker = expectedDisconnectTracker,
            coroutineScope = getFrontendCoroutineScope(),
        ).also { it.install() }

        val clientConnectionStateJob = clientConnectionState.let { state ->
            getFrontendCoroutineScope().launch {
                state.asFlow().collect {
                    val expectedReason = it.expectedDisconnectReason() ?: return@collect
                    expectedDisconnectTracker.mark(expectedReason)
                }
            }
        }

        val hostRestartJob = getFrontendCoroutineScope().launch {
            LinkConnectionUtil.hostRestartEvent().collect {
                expectedDisconnectTracker.mark(ExpectedDisconnectReason.HOST_RESTART)
            }
        }

        project.lifetime.onTermination {
            clientConnectionStateJob?.cancel()
            hostRestartJob.cancel()
            systemSleepMarker.dispose()
        }

        return systemSleepMarker
    }

    private fun ConnectionStateKind.expectedDisconnectReason(): ExpectedDisconnectReason? {
        return when (this) {
            is ConnectionStateKind.Declined -> reason.expectedDisconnectReason()
            is ConnectionStateKind.Terminated -> exitCode.toConnectionCode()?.expectedDisconnectReason()
            else -> null
        }
    }

    private fun ConnectionDeclineReason.expectedDisconnectReason(): ExpectedDisconnectReason {
        return when (this) {
            ConnectionDeclineReason.HostExit -> ExpectedDisconnectReason.HOST_EXIT
            else -> ExpectedDisconnectReason.CONNECTION_DECLINED
        }
    }

    private fun ConnectionCode.expectedDisconnectReason(): ExpectedDisconnectReason? {
        return when (this) {
            ConnectionCode.USER_CANCELED,
            ConnectionCode.CLIENT_DISCONNECTED -> ExpectedDisconnectReason.USER_DISCONNECT

            ConnectionCode.HOST_EXIT -> ExpectedDisconnectReason.HOST_EXIT

            ConnectionCode.FREE_SESSION_ENDED,
            ConnectionCode.KICKED_BY_HOST,
            ConnectionCode.HOST_SESSION_ENDED -> ExpectedDisconnectReason.HOST_SESSION_ENDED

            ConnectionCode.CONNECTION_DECLINED,
            ConnectionCode.GUEST_LIMIT_REACHED,
            ConnectionCode.OTHER_CONTROLLER_LAUNCHED,
            ConnectionCode.SESSION_NOT_FOUND -> ExpectedDisconnectReason.CONNECTION_DECLINED

            else -> null
        }
    }

    private fun Int.toConnectionCode(): ConnectionCode? {
        return ConnectionCode.entries.singleOrNull { it.value == this }
    }

    companion object {
        private const val RECONNECTION_SPAN_NAME = "connection-dropped-reconnecting"
    }
}
