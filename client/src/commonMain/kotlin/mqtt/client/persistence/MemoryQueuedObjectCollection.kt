@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import io.ktor.http.Url
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.MqttUtf8String

class MemoryQueuedObjectCollection : QueuedObjectCollection {
    private var map = HashMap<UShort, ControlPacket>()
    override suspend fun open(clientId: MqttUtf8String, server: Url) {
        map = HashMap()
    }

    override suspend fun keys(limit: UShort): Collection<UShort> = map.keys.sorted()
    override suspend fun put(key: UShort, value: ControlPacket) = map.put(key, value)
    override suspend fun get(key: UShort) = map[key]
    override suspend fun remove(key: UShort) = map.remove(key)
    override suspend fun clear() = map.clear()
    override suspend fun dequeue() = keys(1.toUShort()).firstOrNull()?.let { get(it) }
}
