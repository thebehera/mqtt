package mqtt.transport.nio2.socket

import mqtt.transport.nio.socket.BaseServerSocket
import mqtt.transport.nio2.util.aAccept
import mqtt.transport.nio2.util.aBind
import mqtt.transport.nio2.util.openAsyncServerSocketChannel
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import kotlin.time.ExperimentalTime


@ExperimentalUnsignedTypes
@ExperimentalTime
class AsyncServerSocket : BaseServerSocket<AsynchronousServerSocketChannel>() {
    override suspend fun accept() = AsyncServerToClientSocket(server!!.aAccept())

    override suspend fun bind(channel: AsynchronousServerSocketChannel, socketAddress: SocketAddress?, backlog: UInt) =
        channel.aBind(socketAddress, backlog)

    override suspend fun serverNetworkChannel() = openAsyncServerSocketChannel()

}