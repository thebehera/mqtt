@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.connection.parameters

import androidx.room.*
import mqtt.Parcelize
import mqtt.client.connection.parameters.RemoteHost.Security
import mqtt.client.connection.parameters.RemoteHost.Websocket
import mqtt.connection.IRemoteHost
import mqtt.connection.Milliseconds
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
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


class MqttV4TypeConverters {
    @TypeConverter
    fun fromDirection(direction: DirectionOfFlow): String = direction.name

    @TypeConverter
    fun toDirection(direction: String): DirectionOfFlow = DirectionOfFlow.valueOf(direction)

    @TypeConverter
    fun fromQos(qos: QualityOfService): String = qos.name

    @TypeConverter
    fun toQos(qos: String): QualityOfService = QualityOfService.valueOf(qos)

    @TypeConverter
    fun fromMqttUtf8String(mqttString: MqttUtf8String?): String? = mqttString?.value

    @TypeConverter
    fun toMqttUtf8String(mqttString: String?): MqttUtf8String? =
        if (mqttString == null) null else MqttUtf8String(mqttString)

    @TypeConverter
    fun fromByteArrayWrapper(wrapper: ByteArrayWrapper?): ByteArray? = wrapper?.byteArray

    @TypeConverter
    fun toByteArrayWrapper(bytes: ByteArray?): ByteArrayWrapper? = if (bytes == null) null else ByteArrayWrapper(bytes)

}


