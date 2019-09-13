@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.connection.parameters

import mqtt.Parcelize
import mqtt.connection.ILogConfiguration
import mqtt.connection.IMqttConfiguration
import mqtt.connection.IRemoteHost
import mqtt.connection.IRemoteHost.ISecurityParameters
import mqtt.connection.IRemoteHost.IWebsocketParameters
import mqtt.connection.Milliseconds
import mqtt.persistence.InMemoryPersistence
import mqtt.wire.control.packet.IConnectionRequest

@Parcelize
open class ConnectionParameters(
    override val remoteHost: IRemoteHost,
    override val logConfiguration: ILogConfiguration = LogConfiguration()
) : IMqttConfiguration {
    override fun persistenceLayer() = InMemoryPersistence()
}

@Parcelize
data class RemoteHost(
    override val name: String,
    override val request: IConnectionRequest,
    override val websocket: IWebsocketParameters = Websocket(),
    override val security: ISecurityParameters = Security(),
    override val port: UShort =
        if (websocket.isEnabled) {
            if (security.isTransportLayerSecurityEnabled) {
                443
            } else {
                80
            }
        } else {
            if (security.isTransportLayerSecurityEnabled) {
                8883
            } else {
                1883
            }
        }.toUShort(),
    override val connectionTimeout: Milliseconds = 10_000,
    override val maxNumberOfRetries: Int = Int.MAX_VALUE
) : IRemoteHost {

    @Parcelize
    data class Websocket(override val isEnabled: Boolean = false) : IWebsocketParameters

    @Parcelize
    data class Security(
        override val isTransportLayerSecurityEnabled: Boolean = true,
        override val acceptAllCertificates: Boolean = false
    ) : ISecurityParameters

    override fun hashCode() = connectionIdentifier()
    override fun toString() = uniqueIdentifier()
}

@Parcelize
open class LogConfiguration(
    override val connectionAttempt: Boolean = false,
    override val outgoingPublishOrSubscribe: Boolean = false,
    override val outgoingControlPackets: Boolean = false,
    override val incomingControlPackets: Boolean = false,
    override val incomingPublish: Boolean = false
) : ILogConfiguration
