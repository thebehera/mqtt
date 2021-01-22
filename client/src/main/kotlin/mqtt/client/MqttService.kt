package mqtt.client

import android.app.Service
import android.content.Intent
import android.content.res.AssetFileDescriptor
import kotlinx.coroutines.*
import mqtt.client.persistence.DatabasePersistence
import mqtt.connection.ConnectionOptions
import mqtt.connection.IConnectionOptions
import mqtt.persistence.AndroidContextProvider
import mqtt.persistence.createDriver
import mqtt.wire.buffer.GenericType
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.time.milliseconds

class MqttService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var persistence: DatabasePersistence
    private val clients = mutableMapOf<Long, Client>()

    val binder = object : IRemoteMqttService.Stub() {
        override fun addServer(connectionId: Long, mqttVersion: Byte) {
            scope.launch {
                val connection = persistence.database.connectionsQueries.findConnection(connectionId).executeAsOne()
                val connectionOptions: IConnectionOptions = if (mqttVersion == 4.toByte()) {
                    val connectionRequestWrapper =
                        withContext(Dispatchers.IO) {
                            persistence.database.controlPacketMqtt4Queries.findConnectionRequest(connectionId)
                                .executeAsOne()
                        }
                    val willPayload = connectionRequestWrapper.willPayload
                    val username = connectionRequestWrapper.username
                    val password = connectionRequestWrapper.password
                    val request = ConnectionRequest(
                        ConnectionRequest.VariableHeader(
                            MqttUtf8String(connectionRequestWrapper.protocolName),
                            connectionRequestWrapper.protocolVersion.toByte(),
                            username != null,
                            password != null,
                            connectionRequestWrapper.willRetain == 1L,
                            QualityOfService.from(connectionRequestWrapper.willQos),
                            connectionRequestWrapper.willFlag == 1L,
                            connectionRequestWrapper.cleanSession == 1L,
                            connectionRequestWrapper.keepAliveSeconds.toInt()
                        ),
                        ConnectionRequest.Payload(
                            MqttUtf8String(connectionRequestWrapper.clientId),
                            MqttUtf8String(connectionRequestWrapper.willTopic.toString()),
                            if (willPayload != null) GenericType(willPayload, ByteArray::class) else null,
                            username?.let { MqttUtf8String(it) },
                            password?.let { MqttUtf8String(it) }
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
                val client = Client(connectionOptions, null, persistence, scope = scope)
                clients[connectionId] = client
                client.stayConnectedAsync()
            }
        }

        override fun removeServer(connectionId: Long) {
            scope.launch {
                clients.remove(connectionId)?.disconnectAsync()?.await()
            }
        }

        override fun publish(connectionId: Long, packetId: Long) {
            val client = clients[connectionId] ?: return
            scope.launch {
                val msg = persistence.database.controlPacketMqtt4Queries.findPublish4PacketId(connectionId, packetId)
                    .executeAsOneOrNull() ?: return@launch
                client.publishAsync(
                    msg.topicName,
                    msg.payload?.decodeToString(),
                    QualityOfService.from(msg.qos),
                    msg.dup == 1L,
                    msg.retain == 1L
                ).await()
            }
        }

        override fun publishQos0(connectionId: Long, topicName: String, payload: ByteArray?) {
            val client = clients[connectionId] ?: return
            scope.launch {
                client.publishAsync(topicName, payload?.decodeToString(), QualityOfService.AT_MOST_ONCE).await()
            }
        }


        override fun publishQos0Fd(connectionId: Long, topicName: String?, payload: AssetFileDescriptor?) {

        }

    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            val driver = createDriver("mqtt", AndroidContextProvider(this@MqttService))
            persistence = DatabasePersistence(driver, Dispatchers.Default, 1L)
        }
    }

    override fun onBind(intent: Intent) = binder

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
