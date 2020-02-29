package mqtt.transport.nio2.socket

import kotlinx.coroutines.CoroutineScope
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
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class AsyncServerSocket(
    parentScope: CoroutineScope,
    pool: BufferPool,
    readTimeout: Duration,
    writeTimeout: Duration
) : BaseServerSocket<AsynchronousServerSocketChannel>(parentScope, pool, readTimeout, writeTimeout) {
    override suspend fun accept(channel: AsynchronousServerSocketChannel) = channel.aAccept()

    override suspend fun bind(channel: AsynchronousServerSocketChannel, socketAddress: SocketAddress?) =
        channel.aBind(socketAddress)

    override suspend fun serverNetworkChannel() = openAsyncServerSocketChannel()

    override fun clientToServer(
        scope: CoroutineScope,
        networkChannel: NetworkChannel,
        pool: BufferPool,
        readTimeout: Duration,
        writeTimeout: Duration
    ) = AsyncServerToClientSocket(scope, networkChannel as AsynchronousSocketChannel, pool, readTimeout, writeTimeout)
}