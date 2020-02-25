package mqtt.transport.nio2.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.time.currentTimestampMs
import mqtt.transport.BufferPool
import mqtt.transport.ClientToServerSocket
import mqtt.transport.nio.socket.util.asyncSetOption
import mqtt.transport.nio2.util.aConnect
import mqtt.transport.nio2.util.asyncSocket
import mqtt.transport.util.asInetAddress
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import kotlin.time.*

@ExperimentalTime
val minTimeBeforeLogging = 20.milliseconds

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class AsyncClientSocket(
    coroutineScope: CoroutineScope,
    pool: BufferPool,
    readTimeout: Duration,
    writeTimeout: Duration
) : AsyncBaseClientSocket(coroutineScope, pool, readTimeout, writeTimeout), ClientToServerSocket {

    override suspend fun open(hostname: String?, port: UShort) {
        val totalOpenTime = measureTime {
            val socketAddress = measureTimedValue {
                InetSocketAddress(hostname?.asInetAddress(), port.toInt())
            }
//            println("${currentTimestampMs()} $tag client took ${socketAddress.duration} to resolve")
            val asyncSocket = measureTimedValue { asyncSocket() }
            this.socket = asyncSocket.value
            println("${currentTimestampMs()} $tag client took ${this.socket} ${asyncSocket.duration} to get async socket")
            val configTime = measureTime {
                asyncSocket.value.asyncSetOption(StandardSocketOptions.TCP_NODELAY, true)
                asyncSocket.value.asyncSetOption(StandardSocketOptions.SO_REUSEADDR, false)
                asyncSocket.value.asyncSetOption(StandardSocketOptions.SO_KEEPALIVE, true)
                asyncSocket.value.asyncSetOption(StandardSocketOptions.SO_RCVBUF, 100)
                asyncSocket.value.asyncSetOption(StandardSocketOptions.SO_SNDBUF, 100)
            }
            val connectTime = measureTime {
                asyncSocket.value.aConnect(socketAddress.value, tag)
            }
            if (connectTime > minTimeBeforeLogging) {
                println("${currentTimestampMs()} $tag client ${(asyncSocket.value.remoteAddress as? InetSocketAddress)?.port} took $connectTime to connect  ${asyncSocket.value}")
            }
            val writeChannelStarttime = measureTime { startWriteChannel() }
            if (writeChannelStarttime > minTimeBeforeLogging) {
                println("${currentTimestampMs()} $tag client took $writeChannelStarttime to startt write channel")
            }
        }
    }

}


