package mqtt.client

import kotlinx.coroutines.*
import mqtt.client.connection.ConnectionParameters
import mqtt.client.connection.Open
import mqtt.client.platform.PlatformCoroutineDispatcher
import mqtt.client.session.ClientSession
import mqtt.client.session.ClientSessionState
import kotlin.coroutines.CoroutineContext

class MqttClient(val params: ConnectionParameters) : CoroutineScope {
    private val job: Job = Job()
    private val dispatcher = PlatformCoroutineDispatcher.dispatcher
    val state = ClientSessionState()
    override val coroutineContext: CoroutineContext = job + dispatcher
    var connectionCount = 0
    val session = ClientSession(params, Job(job), state)

    fun startAsync(newConnectionCb: Runnable? = null) = async {
        if (session.transport?.isOpenAndActive() == true) {
            return@async true
        }

        return@async retryIO(params.maxNumberOfRetries) {
            val result = try {
                if (isActive) {
                    val result = session.connect()
                    connectionCount++
                    newConnectionCb?.run()
                    session.awaitSocketClose()
                    result is Open
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
            result
        }
    }

    fun stopAsync() = async {
        session.disconnectAsync()
    }
}