@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import mqtt.Parcelize
import mqtt.connection.IRemoteHost
import mqtt.connection.IRemoteHost.ISecurityParameters
import mqtt.connection.IRemoteHost.IWebsocketParameters
import mqtt.connection.Milliseconds
import mqtt.wire.control.packet.IConnectionRequest

@Parcelize
data class RemoteHost(
    override val name: String,
    override val request: IConnectionRequest,
    override val websocket: IWebsocketParameters = Websocket(),
    override val security: ISecurityParameters = Security(),
    override val port: Int =
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
        },
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
    override fun equals(other: Any?): Boolean {
        if (other !is IRemoteHost) return false
        return connectionIdentifier() == other.connectionIdentifier()
    }
}