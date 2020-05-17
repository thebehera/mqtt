package mqtt.connection

import mqtt.wire.control.packet.IConnectionRequest

interface IMqttConnectionStateUpdated {
    val remoteHostConnectionIdentifier: Int
    val state: ConnectionState
}

data class MqttConnectionStateUpdated(
    override val remoteHostConnectionIdentifier: Int,
    override val state: ConnectionState
) : IMqttConnectionStateUpdated {
    constructor(remote: IRemoteHost, acknowledgment: ConnectionState)
            : this(remote.connectionIdentifier(), acknowledgment)
}

interface IRemoteHost {
    interface IWebsocketParameters {
        val isEnabled: Boolean
    }

    interface ISecurityParameters {
        val isTransportLayerSecurityEnabled: Boolean
        val acceptAllCertificates: Boolean
    }

    val name: String
    val port: Int
    val connectionTimeout: Milliseconds
    val security: ISecurityParameters
    val websocket: IWebsocketParameters
    val request: IConnectionRequest

    val maxNumberOfRetries: Int //= Int.MAX_VALUE

    fun connectionIdentifier() = uniqueIdentifier().hashCode()
    fun uniqueIdentifier(): CharSequence = Companion.uniqueIdentifier(
        request.protocolName, request.protocolVersion,
        request.clientIdentifier, name, port
    )

    companion object {
        fun uniqueIdentifier(
            protocolName: CharSequence,
            protocolVersion: Int,
            clientId: CharSequence,
            name: CharSequence,
            port: Int
        ) =
            listOf(
                protocolName,
                protocolVersion,
                clientId,
                name,
                port
            ).joinToString(".")

        fun connectionIdentifier(
            protocolName: CharSequence,
            protocolVersion: Int,
            clientId: CharSequence,
            name: CharSequence,
            port: Int
        ) = uniqueIdentifier(protocolName, protocolVersion, clientId, name, port).hashCode()
    }
}

typealias Milliseconds = Long
