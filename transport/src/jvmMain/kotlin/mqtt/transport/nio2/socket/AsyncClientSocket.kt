package mqtt.transport.nio2.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.time.currentTimestampMs
import mqtt.transport.BufferPool
import mqtt.transport.ClientToServerSocket
import mqtt.transport.nio2.util.aConnect
import mqtt.transport.nio2.util.asyncSetOption
import mqtt.transport.nio2.util.asyncSocket
import mqtt.transport.util.asInetAddress
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import kotlin.time.*

@ExperimentalTime
val minTimeBeforeLogging = 20.milliseconds

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class AsyncClientSocket(
    coroutineScope: CoroutineScope,
    pool: BufferPool<ByteBuffer>,
    readTimeout: Duration,
    writeTimeout: Duration
) : AsyncBaseClientSocket(coroutineScope, pool, readTimeout, writeTimeout), ClientToServerSocket<ByteBuffer> {
    var tag: String? = null

    override suspend fun open(hostname: String?, port: UShort) {
        val totalOpenTime = measureTime {
            val socketAddress = measureTimedValue {
                InetSocketAddress(hostname?.asInetAddress(), port.toInt())
            }
//            println("${currentTimestampMs()} $tag client took ${socketAddress.duration} to resolve")
            val asyncSocket = measureTimedValue { asyncSocket(group) }
            if (asyncSocket.duration > minTimeBeforeLogging) {
                println("${currentTimestampMs()} $tag client took ${asyncSocket.duration} to get async socket")
            }
            val configTime = measureTime {
                asyncSocket.value.asyncSetOption(StandardSocketOptions.TCP_NODELAY, true)
                asyncSocket.value.asyncSetOption(StandardSocketOptions.SO_REUSEADDR, false)
                asyncSocket.value.asyncSetOption(StandardSocketOptions.SO_KEEPALIVE, true)
                asyncSocket.value.asyncSetOption(StandardSocketOptions.SO_RCVBUF, 100)
                asyncSocket.value.asyncSetOption(StandardSocketOptions.SO_SNDBUF, 100)
            }


//            for (option in asyncSocket.value.supportedOptions()) {
//                println("client socket option $option = ${asyncSocket.value.getOption(option)}")
//            }
//            println("${currentTimestampMs()} $tag client configured($configTime) ${asyncSocket.value}")
            val connectTime = measureTime {
                asyncSocket.value.aConnect(socketAddress.value, tag)
            }
            if (connectTime > minTimeBeforeLogging) {
                println("${currentTimestampMs()} $tag client ${(asyncSocket.value.remoteAddress as? InetSocketAddress)?.port} took $connectTime to connect  ${asyncSocket.value}")
            }
//            println("${currentTimestampMs()} connected, start write channel $socketAddress $asyncSocket")
            val writeChannelStarttime = measureTime { startWriteChannel(asyncSocket.value) }
            if (writeChannelStarttime > minTimeBeforeLogging) {
                println("${currentTimestampMs()} $tag client took $writeChannelStarttime to startt write channel")
            }
//        println("${currentTimestampMs()} write channel started $socketAddress $asyncSocket")
            this.socket = asyncSocket.value
        }
    }

}


