@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.connection.parameters

import androidx.room.*
import mqtt.Parcelize
import mqtt.client.connection.parameters.RemoteHost.Security
import mqtt.client.connection.parameters.RemoteHost.Websocket
import mqtt.connection.IRemoteHost
import mqtt.connection.Milliseconds
import mqtt.wire4.control.packet.PersistableConnectionRequest

@Parcelize
@Entity
@TypeConverters(MqttV4TypeConverters::class)
data class PersistableRemoteHostV4(
    override val name: String,
    @Embedded
    override val request: PersistableConnectionRequest,
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
    override fun toString() = uniqueIdentifier()
}

@Dao
interface RemoteHostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdate(host: PersistableRemoteHostV4)

    @Query("SELECT * FROM PersistableRemoteHostV4")
    suspend fun getAllConnections(): List<PersistableRemoteHostV4>

    @Delete
    suspend fun remove(host: PersistableRemoteHostV4)
}


