package mqtt.connection

import mqtt.Log
import mqtt.NoOpLog
import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.persistence.IQueuedMessage
import mqtt.persistence.MqttPersistence
import mqtt.wire.control.packet.IConnectionRequest

interface IMqttConfiguration : Parcelable {
    val remoteHost: IRemoteHost
    val logConfiguration: ILogConfiguration

    fun persistenceLayer(): MqttPersistence<out IQueuedMessage>
}

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
    fun uniqueIdentifier(): String = listOf(
        request.protocolName,
        request.protocolVersion,
        request.clientIdentifier,
        request.username,
        name,
        port
    ).joinToString(".")
}

interface ILogConfiguration : Parcelable {
    val connectionAttempt: Boolean
    val outgoingPublishOrSubscribe: Boolean
    val outgoingControlPackets: Boolean
    val incomingControlPackets: Boolean
    val incomingPublish: Boolean
    fun getLogClass(): Log = noOpLog

    companion object {
        private val noOpLog by lazy { NoOpLog() }
    }
}

typealias Milliseconds = Long
