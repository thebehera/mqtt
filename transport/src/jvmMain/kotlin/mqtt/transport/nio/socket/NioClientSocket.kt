package mqtt.transport.nio.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.time.currentTimestampMs
import mqtt.transport.BufferPool
import mqtt.transport.ClientToServerSocket
import mqtt.transport.nio.socket.util.aConfigureBlocking
import mqtt.transport.nio.socket.util.asyncSetOption
import mqtt.transport.nio.socket.util.connect
import mqtt.transport.nio.socket.util.openSocketChannel
import mqtt.transport.nio2.socket.minTimeBeforeLogging
import mqtt.transport.util.asInetAddress
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class NioClientSocket(
    coroutineScope: CoroutineScope,
    pool: BufferPool<ByteBuffer>,
    val blocking: Boolean = true,
    readTimeout: Duration,
    writeTimeout: Duration
) : BaseClientSocket(coroutineScope, pool, readTimeout, writeTimeout), ClientToServerSocket<ByteBuffer> {


    override suspend fun open(hostname: String?, port: UShort) {
        val totalOpenTime = measureTime {
            val socketAddress = measureTimedValue {
                InetSocketAddress(hostname?.asInetAddress(), port.toInt())
            }
            val socketChannelTV = measureTimedValue { openSocketChannel() }
            val socketChannel = socketChannelTV.value
            socketChannel.aConfigureBlocking(blocking)


            this.socket = socketChannelTV.value
            println("${currentTimestampMs()} $tag client took ${this.socket} ${socketAddress.duration} to resolve ${socketAddress.value}")
            if (socketChannelTV.duration > minTimeBeforeLogging) {
                println("${currentTimestampMs()} $tag client took ${socketChannelTV.duration} to get async socket")
            }
            val configTime = measureTime {
                socketChannel.asyncSetOption(StandardSocketOptions.TCP_NODELAY, true)
                socketChannel.asyncSetOption(StandardSocketOptions.SO_REUSEADDR, false)
                socketChannel.asyncSetOption(StandardSocketOptions.SO_KEEPALIVE, true)
                socketChannel.asyncSetOption(StandardSocketOptions.SO_RCVBUF, 100)
                socketChannel.asyncSetOption(StandardSocketOptions.SO_SNDBUF, 100)
            }


//            for (option in asyncSocket.value.supportedOptions()) {
//                println("client socket option $option = ${asyncSocket.value.getOption(option)}")
//            }
//            println("${currentTimestampMs()} $tag client configured($configTime) ${asyncSocket.value}")
            val connectTime = measureTime {
                println("about to connect ${socketAddress.value}")
                if (!socketChannel.connect(scope, socketAddress.value, selector, readTimeout)) {
                    println("\"${currentTimestampMs()} $tag  FAILED TO CONNECT CLIENT client ${(socketChannel.remoteAddress as? InetSocketAddress)?.port} $socketChannel")
                }
            }
            if (connectTime > minTimeBeforeLogging) {
                println("${currentTimestampMs()} $tag client ${(socketChannel.remoteAddress as? InetSocketAddress)?.port} took $connectTime to connect  $socketChannel")
            }
//            println("${currentTimestampMs()} connected, start write channel $socketAddress $asyncSocket")
            val writeChannelStarttime = measureTime { startWriteChannel() }
            if (writeChannelStarttime > minTimeBeforeLogging) {
                println("${currentTimestampMs()} $tag client took $writeChannelStarttime to startt write channel")
            }
//        println("${currentTimestampMs()} write channel started $socketAddress $asyncSocket")
        }
    }

}


