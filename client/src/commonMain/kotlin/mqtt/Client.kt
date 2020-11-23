package mqtt

import mqtt.buffer.BufferPool
import mqtt.connection.IRemoteHost
import mqtt.persistence.*
import mqtt.socket.ClientSocket
import mqtt.socket.getClientSocket
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionAcknowledgment
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalUnsignedTypes
@ExperimentalTime
suspend fun connect(remoteHost: IRemoteHost, connectionTimeout: Duration = 1.seconds, pool: BufferPool = BufferPool())
        :ConnectedClient {

    // TODO there should be a way to get a client without being connected to the internet. That way the 

    val socket = getClientSocket(pool)
    socket.open(port = remoteHost.port.toUShort(), hostname = remoteHost.name)
    socket.write(remoteHost.request, connectionTimeout)
    val packets = socket.read(remoteHost.request.controlPacketFactory, connectionTimeout).toMutableList()
    val connack = packets.removeFirst() as IConnectionAcknowledgment
    val client = Client(remoteHost, connack, socket)
    return ConnectedClient(client, packets)
}

@ExperimentalTime
class Client(val remote: IRemoteHost, val connack: IConnectionAcknowledgment, val socket: ClientSocket) {

    fun pub(){}
    fun sub(){}
    fun req(){}
}

@ExperimentalTime
class ConnectedClient(val client: Client, val packetsReceived: Collection<ControlPacket>)

suspend fun buildDatabase(contextProvider: ContextProvider): PlatformDatabase {
    val db = getPlatformDatabase("mqtt", contextProvider)
    val tables = HashMap<String, Row>()
    
    db.open(tables)
    return db
}