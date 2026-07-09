package com.jetbrains.otp.connection

import com.intellij.codeWithMe.ClientId
import com.intellij.platform.split.connection.protocol.ConnectionState
import com.intellij.platform.split.connection.protocol.transport.WireTransportListener
import com.intellij.platform.split.connection.protocol.transport.creator.TransportInfo
import java.util.concurrent.ConcurrentHashMap

class OtpWireTransportListener : WireTransportListener {
    private val stateByClientId = ConcurrentHashMap<String, ReconnectionState>()
    private val lastConnectionStateByClientId = ConcurrentHashMap<String, ConnectionState>()

    override fun transportConnected(clientId: ClientId, transport: TransportInfo) {
        lastConnectionStateByClientId[clientId.value] = ConnectionState.CONNECTED
        clientState(clientId).connected(connectedContext(clientId))
    }

    override fun transportDisconnected(clientId: ClientId, transport: TransportInfo) {
        val lastState = lastConnectionStateByClientId[clientId.value]
        handleDisconnectedState(clientId, lastState, TRANSPORT_DISCONNECTED_STATE)
    }

    override fun transportConnectionStateChanged(
        clientId: ClientId,
        transport: TransportInfo,
        state: ConnectionState,
    ) {
        lastConnectionStateByClientId[clientId.value] = state
        val clientState = clientState(clientId)
        when (state) {
            ConnectionState.CONNECTED -> clientState.connected(connectedContext(clientId))
            is ConnectionState.FAULTED -> clientState.disconnected(faultedContext(clientId, state.throwable))
            ConnectionState.CLOSED -> clientState.expectedDisconnected(
                ExpectedDisconnectReason.TRANSPORT_CLOSED,
                disconnectedContext(clientId, CLOSED_STATE),
            )
            is ConnectionState.DECLINED -> clientState.expectedDisconnected(
                ExpectedDisconnectReason.CONNECTION_DECLINED,
                disconnectedContext(clientId, declinedState(state)),
            )
            ConnectionState.NOT_CONNECTED -> {}
        }
    }

    private fun handleDisconnectedState(
        clientId: ClientId,
        lastState: ConnectionState?,
        fallbackState: String,
    ) {
        val clientState = clientState(clientId)
        when (lastState) {
            is ConnectionState.FAULTED -> clientState.disconnected(faultedContext(clientId, lastState.throwable))
            ConnectionState.CLOSED -> clientState.expectedDisconnected(
                ExpectedDisconnectReason.TRANSPORT_CLOSED,
                disconnectedContext(clientId, CLOSED_STATE),
            )
            is ConnectionState.DECLINED -> clientState.expectedDisconnected(
                ExpectedDisconnectReason.CONNECTION_DECLINED,
                disconnectedContext(clientId, declinedState(lastState)),
            )
            else -> clientState.disconnected(disconnectedContext(clientId, fallbackState))
        }
    }

    private fun clientState(clientId: ClientId): ReconnectionState {
        return stateByClientId.computeIfAbsent(clientId.value) { ReconnectionState(RECONNECTION_SPAN_CONFIG) }
    }

    private fun connectedContext(clientId: ClientId): Map<String, String> {
        return mapOf(
            CLIENT_ID_ATTRIBUTE to clientId.value
        )
    }

    private fun disconnectedContext(clientId: ClientId, state: String): Map<String, String> {
        return connectedContext(clientId) + (CONNECTION_STATE_ATTRIBUTE to state)
    }

    private fun faultedContext(clientId: ClientId, throwable: Throwable): Map<String, String> {
        return buildMap {
            putAll(disconnectedContext(clientId, FAULTED_STATE))
            put(ERROR_CLASS_ATTRIBUTE, throwable::class.java.name)
            throwable.message?.let { put(ERROR_MESSAGE_ATTRIBUTE, it) }
        }
    }

    private fun declinedState(state: ConnectionState.DECLINED): String {
        return "$DECLINED_STATE:${state.reason.name}"
    }

    private companion object {
        const val RECONNECTION_SPAN_NAME = "backend-connection-dropped-reconnecting"

        const val CLIENT_ID_ATTRIBUTE = "client.id"
        const val CONNECTION_STATE_ATTRIBUTE = "reconnection.connection.state"
        const val ERROR_CLASS_ATTRIBUTE = "reconnection.error.class"
        const val ERROR_MESSAGE_ATTRIBUTE = "reconnection.error.message"

        const val CLOSED_STATE = "closed"
        const val DECLINED_STATE = "declined"
        const val FAULTED_STATE = "faulted"
        const val TRANSPORT_DISCONNECTED_STATE = "transport_disconnected"

        val RECONNECTION_SPAN_CONFIG = ReconnectionSpanConfig(
            spanName = RECONNECTION_SPAN_NAME,
        )
    }
}
