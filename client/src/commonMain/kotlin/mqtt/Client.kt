package mqtt

import mqtt.buffer.BufferPool
import mqtt.connection.IRemoteHost
import mqtt.persistence.PlatformDatabase
import mqtt.socket.getClientSocket
import mqtt.wire.control.packet.ControlPacket
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalUnsignedTypes
@ExperimentalTime
suspend fun connect(remoteHost: IRemoteHost, connectionTimeout: Duration = 1.seconds, pool: BufferPool = BufferPool())
        :Collection<ControlPacket> {
    val socket = getClientSocket(pool)
    socket.open(port = remoteHost.port.toUShort(), hostname = remoteHost.name)
    socket.write(remoteHost.request, connectionTimeout)
    return socket.read(remoteHost.request.controlPacketFactory, connectionTimeout)
}

suspend fun buildDatabase(): PlatformDatabase {

}