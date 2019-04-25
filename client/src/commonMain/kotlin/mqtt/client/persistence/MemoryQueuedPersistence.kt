package mqtt.client.persistence

import mqtt.wire.control.packet.ControlPacket

class MemoryQueuedPersistence : QueuedPersistence {
    private val backing = LinkedHashSet<ControlPacket>()

    override fun offer(obj: ControlPacket) = backing.add(obj)

    override fun peek(): ControlPacket? = backing.firstOrNull()

    override fun remove(packet: ControlPacket) = backing.remove(packet)
}