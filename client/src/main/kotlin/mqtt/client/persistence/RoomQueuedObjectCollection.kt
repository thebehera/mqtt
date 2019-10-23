package mqtt.client.persistence

import mqtt.client.connection.parameters.PersistableRemoteHostV4
import mqtt.wire.control.packet.ControlPacket

abstract class RoomQueuedObjectCollection(
    open val db: IMqttConnectionsDb,
    override val remoteHost: PersistableRemoteHostV4
) : QueuedObjectCollection {

    override suspend fun open() {
        db.remoteHostsDao().addOrUpdate(remoteHost)
    }

    protected suspend fun nextQueuedObj(messageId: Int? = null) = if (messageId != null) {
        db.mqttQueueDao().getByMessageId(messageId, remoteHost.connectionId)
    } else {
        db.mqttQueueDao().getNext(remoteHost.connectionId)
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

