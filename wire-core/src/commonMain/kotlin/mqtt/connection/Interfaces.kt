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

data class RemoteHost(
    override val name: String,
    override val port: Int,
    override val request: IConnectionRequest,
    override val security: SecurityParameters = SecurityParameters(false, false),
    override val websocket: WebsocketParameters = WebsocketParameters(false),
    override val connectionTimeout: Milliseconds = 1000,
) : IRemoteHost {
    data class WebsocketParameters(override val isEnabled: Boolean) : IRemoteHost.IWebsocketParameters
    data class SecurityParameters(
        override val isTransportLayerSecurityEnabled: Boolean,
        override val acceptAllCertificates: Boolean
    ) : IRemoteHost.ISecurityParameters
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
    }
}

typealias Milliseconds = Long
