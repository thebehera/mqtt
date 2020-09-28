@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import kotlinx.coroutines.Job
import mqtt.connection.IRemoteHost
import mqtt.wire.control.packet.ControlPacket
import kotlin.coroutines.CoroutineContext

class MemoryQueuedObjectCollection(override val connectionId: Int) : QueuedObjectCollection {
    override val coroutineContext: CoroutineContext = Job()

    private var map = HashMap<UShort, ControlPacket>()
    override suspend fun open(remoteHost: IRemoteHost) {
        map = HashMap()
    }

    override suspend fun get(packetId: Int?): ControlPacket? {
        val lowestMessageId = map.keys.minOrNull() ?: return null
        return map[lowestMessageId]
    }

    override suspend fun ackMessageIdQueueControlPacket(ackMsgId: Int, key: UShort, value: ControlPacket) {
        remove(ackMsgId.toUShort())
        map[key] = value
    }

    override suspend fun remove(key: UShort) {
        map.remove(key)
    }
}
