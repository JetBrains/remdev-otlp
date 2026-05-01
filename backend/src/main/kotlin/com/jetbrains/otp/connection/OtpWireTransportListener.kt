package com.jetbrains.otp.connection

import com.intellij.codeWithMe.ClientId
import com.intellij.platform.split.connection.protocol.ConnectionState
import com.intellij.platform.split.connection.protocol.ConnectionState.Companion.isAlive
import com.intellij.platform.split.connection.protocol.transport.WireTransportListener
import com.intellij.platform.split.connection.protocol.transport.creator.TransportInfo
import java.util.concurrent.ConcurrentHashMap

class OtpWireTransportListener : WireTransportListener {
    private val stateByClientId = ConcurrentHashMap<String, ReconnectionState>()

    override fun transportConnected(clientId: ClientId, transport: TransportInfo) {
        clientState(clientId).connected(connectedContext(clientId))
    }

    override fun transportDisconnected(clientId: ClientId, transport: TransportInfo) {
        clientState(clientId).disconnected()
    }

    override fun transportConnectionStateChanged(
        clientId: ClientId,
        transport: TransportInfo,
        state: ConnectionState,
    ) {
        val clientState = clientState(clientId)
        when {
            state == ConnectionState.CONNECTED -> clientState.connected(connectedContext(clientId))
            !state.isAlive -> clientState.disconnected()
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

    private companion object {
        const val RECONNECTION_SPAN_NAME = "backend-connection-dropped-reconnecting"

        const val CLIENT_ID_ATTRIBUTE = "client.id"

        val RECONNECTION_SPAN_CONFIG = ReconnectionSpanConfig(
            spanName = RECONNECTION_SPAN_NAME,
        )
    }
}
