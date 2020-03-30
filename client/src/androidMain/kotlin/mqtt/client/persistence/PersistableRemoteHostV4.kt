@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.android.parcel.Parcelize
import mqtt.client.RemoteHost.Security
import mqtt.client.RemoteHost.Websocket
import mqtt.connection.IRemoteHost
import mqtt.connection.Milliseconds
import mqtt.wire4.control.packet.ConnectionRequest

@Parcelize
@Entity
@TypeConverters(MqttV4TypeConverters::class)
data class PersistableRemoteHostV4(
    override val name: String,
    @Embedded
    override val request: ConnectionRequest,
    @Embedded
    override val websocket: Websocket = Websocket(),
    @Embedded
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
    @PrimaryKey
    val connectionId: Int = IRemoteHost.connectionIdentifier(
        request.protocolName, request.protocolVersion,
        request.clientIdentifier, name, port
    )
) : IRemoteHost {
    override fun hashCode() = connectionIdentifier()
    override fun toString() = uniqueIdentifier().toString()
}


