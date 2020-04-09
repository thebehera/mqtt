package mqtt.client.session.transport

import kotlinx.coroutines.CoroutineScope
import mqtt.buffer.BufferPool
import mqtt.connection.IRemoteHost
import kotlin.time.ExperimentalTime

@ExperimentalTime
actual suspend fun CoroutineScope.openMqttNetworkSession(
    remoteHost: IRemoteHost,
    pool: BufferPool
): MqttTransport = MqttTransport.openConnection(this, remoteHost, pool)