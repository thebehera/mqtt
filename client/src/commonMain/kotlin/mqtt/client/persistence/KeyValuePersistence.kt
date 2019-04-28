@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import io.ktor.http.Url
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.MqttUtf8String

interface KeyValuePersistence {
    suspend fun open(clientId: MqttUtf8String, server: Url)
    suspend fun containsKey(key: UShort): Boolean
    suspend fun keys(): Collection<UShort>
    suspend fun put(key: UShort, value: ControlPacket): ControlPacket?
    suspend fun get(key: UShort): ControlPacket?
    suspend fun remove(key: UShort): ControlPacket?
    suspend fun clear()
    suspend fun close()
}
