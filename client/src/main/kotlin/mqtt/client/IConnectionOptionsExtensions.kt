package mqtt.client

import android.util.Log
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mqtt.client.persistence.DatabasePersistence
import mqtt.connection.ConnectionOptions
import mqtt.connection.IConnectionOptions
import mqtt.persistence.db.MqttConnections
import mqtt.wire.buffer.GenericType
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.time.milliseconds
import kotlin.time.seconds

suspend fun buildConnectionOptions(connection: MqttConnections, persistence: DatabasePersistence): IConnectionOptions {
    val connectionId = connection.connectionId
    return if (connection.version == 4L) {
        val connectionRequestWrapper =
            withContext(Dispatchers.IO) {
                persistence.database.controlPacketMqtt4Queries.findConnectionRequest(connectionId)
                    .executeAsOne()
            }
        val willPayload = connectionRequestWrapper.willPayload
        val username = connectionRequestWrapper.username
        val password = connectionRequestWrapper.password
        val minIntervalTime = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS.milliseconds
        println("${connectionRequestWrapper.keepAliveSeconds.seconds} $minIntervalTime")
        val keepAlive = if (connectionRequestWrapper.keepAliveSeconds.seconds >= minIntervalTime) {
            connectionRequestWrapper.keepAliveSeconds.seconds
        } else {
            Log.w("[ICOE]", "Keep alive timer set too low (${connectionRequestWrapper.keepAliveSeconds.seconds}), as system will not be able to schedule below $minIntervalTime with a min flex time of ${PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS.milliseconds}. Increasing keep alive timeout to $minIntervalTime.")
            minIntervalTime
        }

        val request = ConnectionRequest(
            ConnectionRequest.VariableHeader(
                connectionRequestWrapper.protocolName,
                connectionRequestWrapper.protocolVersion.toByte(),
                username != null,
                password != null,
                connectionRequestWrapper.willRetain == 1L,
                QualityOfService.from(connectionRequestWrapper.willQos),
                connectionRequestWrapper.willFlag == 1L,
                connectionRequestWrapper.cleanSession == 1L,
                keepAlive.inSeconds.toInt()
            ),
            ConnectionRequest.Payload(
                connectionRequestWrapper.clientId,
                connectionRequestWrapper.willTopic,
                if (willPayload != null) GenericType(willPayload, ByteArray::class) else null,
                username,
                password
            )
        )
        ConnectionOptions(
            connection.name,
            connection.port.toInt(),
            request,
            connection.connectionTimeoutMs.milliseconds,
            connection.websocketEndpoint
        )
    } else {
        throw UnsupportedOperationException("mqtt 5 not implemented yet")
    }
}