@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import io.ktor.http.Url
import mqtt.wire.MqttPersistenceException
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.MqttUtf8String

class MemoryKeyValuePersistence : KeyValuePersistence {
    private var map: MutableMap<UShort, ControlPacket>? = null
    private fun getMapIfOpen() = this.map ?: throw MqttPersistenceException("KeyValuePersistence is closed")
    override fun open(clientId: MqttUtf8String, server: Url) {
        map = HashMap()
    }

    override fun containsKey(key: UShort) = getMapIfOpen().containsKey(key)
    override fun keys(): Set<UShort> = getMapIfOpen().keys
    override fun put(key: UShort, value: ControlPacket) = getMapIfOpen().put(key, value)
    override fun get(key: UShort) = getMapIfOpen()[key]
    override fun remove(key: UShort) = getMapIfOpen().remove(key)
    override fun clear() = getMapIfOpen().clear()
    override fun close() {
        map = null
    }
}
