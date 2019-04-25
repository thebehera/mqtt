package mqtt.client.persistence

import mqtt.wire.control.packet.ControlPacket

interface QueuedPersistence {
    /**
     * Inserts the specified element into this queue if it is possible to do so immediately without violating capacity
     * restrictions.
     */
    fun offer(obj: ControlPacket): Boolean

    /**
     * Return a packet off the queue
     */
    fun peek(): ControlPacket?

    /**
     * Removes the packet if found
     */
    fun remove(packet: ControlPacket): Boolean
}