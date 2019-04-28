@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import io.ktor.http.Url
import mqtt.wire.MqttPersistenceException
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.MqttUtf8String

class MemoryKeyValuePersistence : KeyValuePersistence {
    private var map: MutableMap<UShort, ControlPacket>? = null
    private fun getMapIfOpen() = this.map ?: throw MqttPersistenceException("KeyValuePersistence is closed")
    override suspend fun open(clientId: MqttUtf8String, server: Url) {
        map = HashMap()
    }

    override suspend fun containsKey(key: UShort) = getMapIfOpen().containsKey(key)
    override suspend fun keys(): Set<UShort> = getMapIfOpen().keys
    override suspend fun put(key: UShort, value: ControlPacket) = getMapIfOpen().put(key, value)
    override suspend fun get(key: UShort) = getMapIfOpen()[key]
    override suspend fun remove(key: UShort) = getMapIfOpen().remove(key)
    override suspend fun clear() = getMapIfOpen().clear()
    override suspend fun close() {
        map = null
    }
}
