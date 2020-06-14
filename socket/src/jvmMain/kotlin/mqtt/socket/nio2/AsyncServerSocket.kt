package mqtt.socket.nio2

import mqtt.socket.nio.BaseServerSocket
import mqtt.socket.nio2.util.aAccept
import mqtt.socket.nio2.util.aBind
import mqtt.socket.nio2.util.openAsyncServerSocketChannel
import java.net.SocketAddress
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.ClosedChannelException
import kotlin.time.ExperimentalTime


@ExperimentalUnsignedTypes
@ExperimentalTime
class AsyncServerSocket : BaseServerSocket<AsynchronousServerSocketChannel>() {
    override suspend fun accept(): AsyncServerToClientSocket? {
        try {
            return AsyncServerToClientSocket(server!!.aAccept())
        } catch (e: ClosedChannelException) {
            // This will happen when the server socket is closed.
        }
        return null
    }

    override suspend fun bind(channel: AsynchronousServerSocketChannel, socketAddress: SocketAddress?, backlog: UInt) =
        channel.aBind(socketAddress, backlog)

    override suspend fun serverNetworkChannel() = openAsyncServerSocketChannel()

}