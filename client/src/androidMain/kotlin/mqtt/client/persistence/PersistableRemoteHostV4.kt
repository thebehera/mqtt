@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import mqtt.client.RemoteHost.Security
import mqtt.client.RemoteHost.Websocket
import mqtt.connection.IRemoteHost
import mqtt.connection.Milliseconds
import mqtt.wire4.control.packet.ConnectionRequest

data class PersistableRemoteHostV4(
    override val name: String,
    override val request: ConnectionRequest<Unit>,
    override val websocket: Websocket = Websocket(),
    override val security: Security = Security(),
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
    override val maxNumberOfRetries: Int = Int.MAX_VALUE,
    val connectionId: Int = IRemoteHost.connectionIdentifier(
        request.protocolName, request.protocolVersion,
        request.clientIdentifier, name, port
    )
) : IRemoteHost {
    override fun hashCode() = connectionIdentifier()
    override fun toString() = uniqueIdentifier().toString()
}


