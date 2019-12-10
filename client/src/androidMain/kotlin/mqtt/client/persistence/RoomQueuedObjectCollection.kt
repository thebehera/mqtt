package mqtt.client.persistence

import mqtt.connection.IRemoteHost
import mqtt.wire.control.packet.ControlPacket

abstract class RoomQueuedObjectCollection(
    open val db: IMqttConnectionsDb,
    override val connectionId: Int
) : QueuedObjectCollection {

    override suspend fun open(remoteHost: IRemoteHost) {
        db.remoteHostsDao().addOrUpdate(remoteHost as PersistableRemoteHostV4)
    }

    protected suspend fun nextQueuedObj(packetId: Int? = null) = if (packetId != null) {
        db.mqttQueueDao().getByMessageId(packetId, connectionId)
    } else {
        db.mqttQueueDao().getNext(connectionId)
    }

    override suspend fun ackMessageIdQueueControlPacket(ackMsgId: Int, key: UShort, value: ControlPacket) {
        db.mqttQueueDao().acknowledge(ackMsgId)
        // TODO: Implement the queing of the next control packet
        println("TODO: Implement the queing of the next control packet")
    }

    override suspend fun remove(key: UShort) {
        db.mqttQueueDao().acknowledge(key.toInt())
    }
}

