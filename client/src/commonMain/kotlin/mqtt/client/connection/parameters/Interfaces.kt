package mqtt.client.connection.parameters

import mqtt.Log
import mqtt.NoOpLog
import mqtt.Parcelable
import mqtt.wire.control.packet.IConnectionRequest

interface IMqttConfiguration : Parcelable {
    val remoteHost: IRemoteHost
    val logConfiguration: ILogConfiguration
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
    val port: UShort
    val connectionTimeout: Milliseconds
    val security: ISecurityParameters
    val websocket: IWebsocketParameters
    val request: IConnectionRequest

    val maxNumberOfRetries: Int //= Int.MAX_VALUE
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