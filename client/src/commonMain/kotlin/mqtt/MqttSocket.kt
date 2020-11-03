package mqtt

import mqtt.socket.ClientSocket
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketFactory
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
suspend fun ClientSocket.write(packet: ControlPacket, timeout: Duration) {
    pool.borrowSuspend(packet.packetSize()) { buffer ->
        packet.serialize(buffer)
        writeFully(buffer, timeout)
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