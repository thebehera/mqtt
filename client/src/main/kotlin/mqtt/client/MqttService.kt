package mqtt.client

import android.app.Service
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.os.RemoteException
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mqtt.client.persistence.DatabasePersistence
import mqtt.client.socket.ConnectionState
import mqtt.connection.IConnectionOptions
import mqtt.persistence.AndroidContextProvider
import mqtt.persistence.createDriver
import mqtt.persistence.db.Database
import mqtt.persistence.db.MqttConnections
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionAcknowledgment
import mqtt.wire.control.packet.SubscriptionWrapper
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Filter
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.minutes
import kotlin.time.seconds

class MqttService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clients = mutableMapOf<Long, Client>()
    private val persistence = mutableMapOf<Long, DatabasePersistence>()
    private val workManager by lazy {
        WorkManager.getInstance(this)
    }
    private lateinit var database: Database
    private val incomingMessageObservers = mutableSetOf<ControlPacketCallback>()
    private val outgoingMessageObservers = mutableSetOf<ControlPacketCallback>()
    private val currentConnectionJob = mutableMapOf<Long, Job>()
    private val connectFlow = Channel<MqttConnections>()

    suspend fun connect(connection: MqttConnections) {
        connectFlow.send(connection)
    }

    private suspend fun connectLocal(connection: MqttConnections) {
        if (persistence.containsKey(connection.connectionId)) {
            return
        }
        persistence[connection.connectionId] = DatabasePersistence(database, Dispatchers.IO, connection.connectionId)
        val connectionId = connection.connectionId
        val persistence = findPersistence(connectionId)
        val connectionOptions: IConnectionOptions = buildConnectionOptions(connection, persistence)
        val callback = object : ApplicationMessageCallback {
            override suspend fun onMessageReceived(msg: ControlPacket) {
                notifyObserverOfMessage(msg, incomingMessageObservers)
            }
            override suspend fun onMessageSent(msg: ControlPacket) {
                notifyObserverOfMessage(msg, outgoingMessageObservers)
            }
        }
        val client = Client(connectionOptions, callback, persistence, scope, false)
        val keepAlive = connectionOptions.request.keepAliveTimeout
        val keepAliveLongMs = keepAlive.toLong(MILLISECONDS)
        val keepAliveFlex = keepAliveLongMs.div(2)
        val clientWorkRequest = PeriodicWorkRequestBuilder<PingWorker>(
            keepAliveLongMs, MILLISECONDS,
            keepAliveFlex, MILLISECONDS
        )
            .addTag("mqtt:ping:$connectionId")
            .addTag("mqtt:ping")
//            .setInitialDelay(keepAliveLongMs, MILLISECONDS)
            .build()

        client.onConnectionStateCallback = {
            when (it) {
                is ConnectionState.Connected -> {
                    val worker = PingWorker.createForegroundInfo(this, null, "Connected", "Connected")
                    if (PingWorker.shouldForeground)
                        startForeground(PingWorker.notificationId, worker.notification)
                    else
                        NotificationManagerCompat.from(this).notify(PingWorker.notificationId, worker.notification)
                    workManager.enqueueUniquePeriodicWork("mqtt:ping:$connectionId", ExistingPeriodicWorkPolicy.REPLACE, clientWorkRequest)
                }
                ConnectionState.Disconnected -> {
                    workManager.cancelAllWorkByTag("mqtt:ping:$connectionId")
                }
                is ConnectionState.Reconnecting -> {
                    // do nothing
                }
            }
        }
        clients[connectionId] = client
        currentConnectionJob[connectionId] = client.stayConnectedAsync()
    }

    private val binder = object : IRemoteMqttService.Stub() {

        override fun addServer(connectionId: Long, mqttVersion: Byte) {
            scope.launch {
                val connection = database.connectionsQueries.findConnection(connectionId).executeAsOne()
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

        override fun subscribe(connectionId: Long, topic: Array<out String>, qos: IntArray) {
            println("sub client ${clients[connectionId]}")
            val client = clients[connectionId] ?: return
            scope.launch {
                val subscriptionsWrapped = topic.mapIndexed { index, topic ->
                    SubscriptionWrapper(Filter(topic), QualityOfService.from(qos[index].toLong()))
                }.toSet()
                println("subscribe to: $subscriptionsWrapped")
                client.subscribeAsync(subscriptionsWrapped)
            }
        }

        override fun unsubscribe(connectionId: Long, topic: Array<out String>) {
            val client = clients[connectionId] ?: return
            scope.launch {
                client.unsubscribeAsync(*topic)
            }
        }


        override fun publishQos0Fd(connectionId: Long, topicName: String?, payload: AssetFileDescriptor?) {

        }

        override fun ping(): LongArray {
            val list = mutableListOf<Long>()
            val jobs = mutableSetOf<Job>()
            for ((connectionId, client) in clients) {
                jobs += scope.launch {
                    if (client.connectionState !is ConnectionState.Connected) {
                        println("reconnecting pingy")
                        currentConnectionJob[connectionId]?.cancel()
                        currentConnectionJob[connectionId] = client.stayConnectedAsync()
                        client.suspendUntilMessage { it is IConnectionAcknowledgment }
                        println("reconnected pingy")
                    } else {
                        client.writePing()
                    }
                    list += connectionId
                }
            }
            runBlocking(Dispatchers.Default) {
                jobs.joinAll()
            }
            return list.toLongArray()
        }

        override fun addIncomingMessageCallback(callback: ControlPacketCallback) {
            incomingMessageObservers += callback
        }

        override fun removeIncomingMessageCallback(callback: ControlPacketCallback) {
            incomingMessageObservers -= callback
        }

        override fun addOutgoingMessageCallback(callback: ControlPacketCallback) {
            outgoingMessageObservers += callback
        }

        override fun removeOutgoingMessageCallback(callback: ControlPacketCallback) {
            outgoingMessageObservers -= callback
        }

        override fun resetReconnectTimer() {
            clients.forEach {  (connectionId, client) ->
                client.currentDelay = 0.seconds
                if (client.connectionState !is ConnectionState.Connected) {
                    currentConnectionJob[connectionId]?.cancel()
                    currentConnectionJob[connectionId] = client.stayConnectedAsync()
                }
            }
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
        scope.launch {
            launch {
                registerNetworkListener(this@MqttService)
                for (connection in connectFlow) {
                    connectLocal(connection)
                }
            }
            val workConfig = Configuration.Builder()
            workConfig.setMinimumLoggingLevel(Log.VERBOSE)
            workConfig.setDefaultProcessName(applicationInfo.processName)
            WorkManager.initialize(this@MqttService, workConfig.build())
            database = Database(createDriver("mqtt", AndroidContextProvider(this@MqttService)))
            val connections = database.connectionsQueries.getConnections().executeAsList()
            connections.forEach { connect(it) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.getBooleanExtra("ping", true)) {
                scope.launch {
                    binder.ping()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun notifyObserverOfMessage(msg: ControlPacket, callbacks: MutableCollection<ControlPacketCallback>) {
        val deadObservers = mutableListOf<ControlPacketCallback>()
        callbacks.forEach { callback ->
            try {
                callback.onMessage(ControlPacketWrapper().also { it.packet = msg })
            } catch (e: RemoteException) {
                deadObservers += callback
            }
        }
        callbacks.removeAll(deadObservers)
    }

    override fun onBind(intent: Intent) = binder
    override fun onDestroy() {
        Log.i("RAHUL","destroy service")
        super.onDestroy()
        connectFlow.close()
        scope.cancel()
    }
}
