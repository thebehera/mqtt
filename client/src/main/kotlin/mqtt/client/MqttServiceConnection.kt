package mqtt.client

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.coroutines.*
import mqtt.connection.IConnectionOptions
import mqtt.persistence.AndroidContextProvider
import mqtt.persistence.createDriver
import mqtt.persistence.db.Database
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MqttAppServiceConnection private constructor(
    driver: SqlDriver,
    private val service: IRemoteMqttService,
    private val scope: CoroutineScope
) {
    private val database = Database(driver)

    fun addServerAsync(serverOptions: IConnectionOptions) = scope.async {
        val request = serverOptions.request as ConnectionRequest<ByteArray>
        val insertedConnectionId = withContext(Dispatchers.IO) {
            database.transactionWithResult<Long> {
                database.connectionsQueries.addConnection(
                    serverOptions.name,
                    serverOptions.port.toLong(),
                    serverOptions.connectionTimeout.toLongMilliseconds(),
                    serverOptions.websocketEndpoint
                )
                val insertedConnectionId = database.connectionsQueries.lastInsertRowId().executeAsOne()
                database.controlPacketMqtt4Queries.queueConnectionRequest(
                    insertedConnectionId,
                    request.variableHeader.protocolName.toString(),
                    request.variableHeader.protocolLevel.toLong(),
                    request.payload.userName?.toString(),
                    request.payload.password?.toString(),
                    request.payload.clientId.toString(),
                    request.payload.willTopic.toString(),
                    request.payload.willPayload?.obj,
                    if (request.variableHeader.willRetain) 1L else 0L,
                    request.variableHeader.willQos.integerValue.toLong(),
                    if (request.variableHeader.willFlag) 1L else 0L,
                    if (request.variableHeader.cleanSession) 1L else 0L,
                    request.variableHeader.keepAliveSeconds.toLong()
                )
                insertedConnectionId
            }
        }
        try {
            service.addServer(insertedConnectionId, request.mqttVersion)
        } catch (e: RemoteException) {
            Log.e(
                "MqttAppServiceConnection",
                "We lost connection to the service, but that's ok as we already queued the connection",
                e
            )
        }
    }

    fun publishAsync(
        connectionId: Long,
        topicName: String,
        payload: String? = null,
        qos: QualityOfService = QualityOfService.AT_LEAST_ONCE,
        retain: Boolean = false,
        // MQTT 5 Properties
        payloadFormatIndicator: Boolean = false,
        messageExpiryInterval: Long? = null,
        topicAlias: Int? = null,
        responseTopic: CharSequence? = null,
        correlationData: String? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList(),
        subscriptionIdentifier: Set<Long> = emptySet(),
        contentType: CharSequence? = null
    ) = scope.async {
        if (qos == QualityOfService.AT_MOST_ONCE) {
//            service.
        } else {
            database.transactionWithResult<Long> {
                val packetId =
                    database.controlPacketMqtt4Queries.findUnusedPacketIdentifier(connectionId).executeAsOne()
                database.controlPacketMqtt4Queries.publish4(
                    connectionId,
                    packetId,
                    0,
                    qos.integerValue.toLong(),
                    if (retain) 1 else 0,
                    topicName,
                    payload?.toByteArray()
                )
                packetId
            }
        }
    }

    fun removeServerAsync(connectionId: Long) = scope.async {
        database.connectionsQueries.removeConnection(connectionId)
        try {
            service.removeServer(connectionId)
        } catch (e: RemoteException) {
            Log.e(
                "MqttAppServiceConnection",
                "We lost connection to the service, but that's ok as we already queued the connection",
                e
            )
        }
    }

    companion object {
        fun getMqttServiceConnectionAsync(context: Context, parent: CoroutineScope) = parent.async {
            val driver = async { createDriver("mqtt", AndroidContextProvider(context)) }
            val service = suspendCoroutine<IRemoteMqttService> {
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                        it.resume(IRemoteMqttService.Stub.asInterface(binder)!!)
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        cancel()
                    }
                }
            }
            MqttAppServiceConnection(driver.await(), service, this)
        }
    }
}
