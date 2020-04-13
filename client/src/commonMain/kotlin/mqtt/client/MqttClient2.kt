package mqtt.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mqtt.buffer.BufferPool
import mqtt.client.persistence.QueuedObjectCollection
import mqtt.client.session.transport.MqttTransport
import mqtt.connection.IRemoteHost
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@ExperimentalUnsignedTypes
@ExperimentalTime
class MqttClient2(
    val scope: CoroutineScope,
    val remoteHost: IRemoteHost,
    val pool: BufferPool,
    val queuedObjectCollection: QueuedObjectCollection
) {
    var transport: MqttTransport? = null
    val connectingMutex = Mutex()

    suspend fun connect() {
        connectingMutex.withLock {
            if (transport?.socket?.isOpen() == true) {
                return@withLock
            }
            val (transport, _) = MqttTransport.openConnection(scope, remoteHost, pool)
            scope.launchKeepAlive(transport, remoteHost)
            this.transport = transport
        }
    }
}


@ExperimentalTime
fun CoroutineScope.launchKeepAlive(transport: MqttTransport, remoteHost: IRemoteHost) = launch {
    while (transport.isOpen()) {
        delayUntilKeepAlive(transport, remoteHost)
        transport.asyncWrite(remoteHost.request.controlPacketReader.pingRequest())
    }
}

@ExperimentalTime
private suspend fun delayUntilKeepAlive(transport: MqttTransport, remoteHost: IRemoteHost) {
    val keepAliveDuration = remoteHost.request.keepAliveTimeoutSeconds.toInt().toDuration(DurationUnit.SECONDS)
    while (transport.lastMessageReceived.elapsedNow() > keepAliveDuration) {
        delay((transport.lastMessageReceived.elapsedNow() - keepAliveDuration).toLongMilliseconds())
    }
}