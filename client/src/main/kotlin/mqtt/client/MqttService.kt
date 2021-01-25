package mqtt.client

import android.app.Service
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.os.IBinder
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import mqtt.client.persistence.DatabasePersistence
import mqtt.client.persistence.Persistence
import mqtt.connection.ConnectionOptions
import mqtt.connection.IConnectionOptions
import mqtt.persistence.AndroidContextProvider
import mqtt.persistence.createDriver
import mqtt.persistence.db.Database
import mqtt.persistence.db.MqttConnections
import mqtt.wire.buffer.GenericType
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.time.milliseconds

class MqttService : Service() {
    private val scope = MainScope()
    private val clients = mutableMapOf<Long, Client>()
    private val persistence = mutableMapOf<Long, DatabasePersistence>()
    private lateinit var database: Database

    suspend fun connect(connection: MqttConnections) {
        val connectionId = connection.connectionId
        val persistence = findPersistence(connectionId)
        val connectionOptions: IConnectionOptions = if (connection.version == 4L) {
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
                    connectionRequestWrapper.protocolName,
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
        val client = Client(connectionOptions, null, persistence, scope = scope)
        clients[connectionId] = client
        client.stayConnectedAsync()
    }

    private val binder = object : IRemoteMqttService.Stub() {

        override fun addServer(connectionId: Long, mqttVersion: Byte) {
            scope.launch {
                val connection = database.connectionsQueries.findConnection(connectionId).executeAsOne()
                persistence[connection.connectionId] = DatabasePersistence(database, Dispatchers.IO, connection.connectionId)
                connect(connection)
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
                val persistence = findPersistence(connectionId)
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

    private suspend fun findPersistence(connectionId:Long): DatabasePersistence {
        var isNewlyAllocated = false
        val foundPersistence = persistence.getOrPut(connectionId) {
            val persistence = DatabasePersistence(database, Dispatchers.IO, connectionId)
            isNewlyAllocated = true
            persistence
        }
        if (isNewlyAllocated) {
            withContext(Dispatchers.Default) { launch {
                foundPersistence.suspendUntilConnectionRemoval()
                clients.remove(connectionId)?.disconnectAsync()?.await()
                persistence.remove(connectionId)
            } }
        }
        return foundPersistence
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch(Dispatchers.IO) {
            database = Database(createDriver("mqtt", AndroidContextProvider(this@MqttService)))
            val connections = database.connectionsQueries.getConnections().executeAsList()
            connections.forEach { connection ->
                launch(Dispatchers.Default) {
                    persistence[connection.connectionId] = DatabasePersistence(database, Dispatchers.IO, connection.connectionId)
                    connect(connection)
                }
            }
        }
    }
    override fun onBind(intent: Intent) = binder
    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
