package mqtt.client

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.io.errors.IOException
import mqtt.wire.control.packet.ConnectionRequest
import mqtt.wire.control.packet.ControlPacket
import kotlin.coroutines.CoroutineContext


expect fun CoroutineScope.openSocket(
        hostname: String,
        port: Int, connectionRequest: ConnectionRequest,
        clientToBroker: Channel<ControlPacket>,
        brokerToClient: SendChannel<ControlPacket>): Job

abstract class ClientConnection : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext get() = job + dispatcher
    abstract val dispatcher: CoroutineDispatcher
    abstract val hostname: String
    abstract val port: Int
    fun close() = job.cancel()
}

suspend fun <T> retryIO(times: Int = Int.MAX_VALUE, initialDelay: Long = 100, maxDelay: Long = 1000,
                        factor: Double = 2.0, block: suspend () -> T): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: IOException) {
            // you can log an error here and/or make a more finer-grained
            // analysis of the cause to see if retry is needed
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block()
}