package mqtt.connection

import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.wire.control.packet.IConnectionRequest

interface IMqttConnectionStateUpdated : Parcelable {
    val remoteHostConnectionIdentifier: Int
    val state: ConnectionState
}

@Parcelize
data class MqttConnectionStateUpdated(
    override val remoteHostConnectionIdentifier: Int,
    override val state: ConnectionState
) : IMqttConnectionStateUpdated {
    constructor(remote: IRemoteHost, acknowledgment: ConnectionState)
            : this(remote.connectionIdentifier(), acknowledgment)
}

interface IRemoteHost : Parcelable {
    interface IWebsocketParameters : Parcelable {
        val isEnabled: Boolean
    }

    interface ISecurityParameters : Parcelable {
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
    fun uniqueIdentifier(): String = Companion.uniqueIdentifier(
        request.protocolName, request.protocolVersion,
        request.clientIdentifier, name, port
    )

    companion object {
        fun uniqueIdentifier(protocolName: String, protocolVersion: Int, clientId: String, name: String, port: Int) =
            listOf(
                protocolName,
                protocolVersion,
                clientId,
                name,
                port
            ).joinToString(".")

        fun connectionIdentifier(
            protocolName: String,
            protocolVersion: Int,
            clientId: String,
            name: String,
            port: Int
        ) = uniqueIdentifier(protocolName, protocolVersion, clientId, name, port).hashCode()
    }
}

typealias Milliseconds = Long
