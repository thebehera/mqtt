package mqtt.transport.nio2.socket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import mqtt.transport.BufferPool
import mqtt.transport.nio.socket.BaseServerSocket
import mqtt.transport.nio2.util.aAccept
import mqtt.transport.nio2.util.aBind
import mqtt.transport.nio2.util.openAsyncServerSocketChannel
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.NetworkChannel
import kotlin.time.ExperimentalTime


@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class AsyncServerSocket(pool: BufferPool) : BaseServerSocket<AsynchronousServerSocketChannel>(pool) {
    override suspend fun accept(channel: AsynchronousServerSocketChannel) = channel.aAccept()

    override suspend fun bind(channel: AsynchronousServerSocketChannel, socketAddress: SocketAddress?) =
        channel.aBind(socketAddress)

    override suspend fun serverNetworkChannel() = openAsyncServerSocketChannel()

    override fun clientToServer(networkChannel: NetworkChannel) =
        AsyncServerToClientSocket(networkChannel as AsynchronousSocketChannel, pool)
}