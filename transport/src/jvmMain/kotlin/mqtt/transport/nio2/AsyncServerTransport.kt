package mqtt.transport.nio2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mqtt.connection.ServerControlPacketTransport
import mqtt.transport.nio2.util.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
internal class AsyncServerTransport(
    override val scope: CoroutineScope,
    val maxBufferSize: Int,
    val group: AsynchronousChannelGroup? = null
) : ServerControlPacketTransport {

    private lateinit var server: AsynchronousServerSocketChannel
    var localAddress: InetSocketAddress? = null
    private val readBuffer = ByteBuffer.allocateDirect(maxBufferSize)
    private val connections = HashSet<AsyncServerClientTransport>()

    override suspend fun listen(
        port: UShort?,
        host: String,
        readTimeout: Duration
    ): Flow<AsyncServerClientTransport> {
        val server = group.openAsyncServerSocketChannel()
        this.server = if (port != null) {
            server.aBind(InetSocketAddress(host, port.toInt()))
        } else {
            server.aBind(null)
        }
        println("Mock server bound to ${server.localAddress}")
        localAddress = server.localAddress as? InetSocketAddress
        return channelFlow {
            try {
                while (scope.isActive && server.isOpen) {
                    val connection = server.aAccept()
                    scope.launch {
                        val connectionRequest = connection.readConnectionRequest(readBuffer, 1.seconds)
                        if (connectionRequest != null) {
                            val transport =
                                AsyncServerClientTransport(
                                    scope,
                                    connection,
                                    maxBufferSize,
                                    connectionRequest
                                )
                            transport.openChannels()
                            connections.add(transport)
                            send(transport)
                        } else {
                            println("aClose")
                            connection.aClose()
                        }
                    }
                }
            } catch (e: Throwable) {
                println("server closed $e")
            } finally {
                close()
            }
        }
    }


    override fun close() {
        connections.forEach { runBlocking { it.socket.aClose() } }
        try {
            this.server.close()
        } catch (e: Throwable) {

        } finally {
            println("closed server")
        }
    }

}