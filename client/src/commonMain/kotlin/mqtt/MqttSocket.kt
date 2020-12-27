@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt

import mqtt.socket.ClientSocket
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketFactory
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
suspend fun ClientSocket.write(packets: Collection<ControlPacket>, timeout: Duration) {
    val totalBufferSize = packets.fold(0u) { acc, controlPacket ->
        acc + controlPacket.packetSize()
    }
    pool.borrowSuspend(totalBufferSize) { buffer ->
        packets.forEach { packet -> packet.serialize(buffer) }
        write(buffer, timeout)
    }
}

@ExperimentalTime
suspend fun ClientSocket.read(controlPacketFactory: ControlPacketFactory, timeout: Duration): Collection<ControlPacket> {
    return readTyped(timeout) { buffer ->
        val incomingMessages = ArrayList<ControlPacket>()
        try {
            while (buffer.hasRemaining()) {
                incomingMessages += controlPacketFactory.from(buffer)
            }
        } catch (e: Exception) {
            println("W: failed to read remaining buffer for control packets")
            e.printStackTrace()
        }
        incomingMessages
    }
}


//
//@ExperimentalTime
//class ConnectedClient(val client: ClientOLD, val packetsReceived: Collection<ControlPacket>)
//
//
//@ExperimentalUnsignedTypes
//@ExperimentalTime
//suspend fun connect(contextProvider: ContextProvider, remoteHost: IRemoteHost, pool: BufferPool = BufferPool())
//        : ConnectedClient {
//
//    // TODO there should be a way to get a client without being connected to the internet. That way the
//
//    val socket = getClientSocket(pool)
//    socket.open(port = remoteHost.port.toUShort(), hostname = remoteHost.name)
//    socket.write(remoteHost.request, remoteHost.connectionTimeout.milliseconds)
//    val packets =
//        socket.read(remoteHost.request.controlPacketFactory, remoteHost.connectionTimeout.milliseconds).toMutableList()
//    val connack = packets.removeFirst() as IConnectionAcknowledgment
//
//    val client = ClientOLD(contextProvider, remoteHost, connack, socket)
//    return ConnectedClient(client, packets)
//}