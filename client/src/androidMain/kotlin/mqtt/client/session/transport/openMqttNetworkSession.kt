package mqtt.client.session.transport

import mqtt.buffer.BufferPool
import mqtt.connection.IRemoteHost
import kotlin.time.ExperimentalTime

@ExperimentalTime
actual suspend fun openMqttNetworkSession(
    remoteHost: IRemoteHost,
    pool: BufferPool
): MqttNetworkSession = MqttNetworkSession.openConnection(remoteHost, pool)