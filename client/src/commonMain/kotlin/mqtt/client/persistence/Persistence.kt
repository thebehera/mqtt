@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import io.ktor.http.Url
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.MqttUtf8String

interface Persistence {
    fun open(clientId: MqttUtf8String, server: Url)
    fun containsKey(key: UShort): Boolean
    fun keys(): Collection<UShort>
    fun put(key: UShort, value: ControlPacket): ControlPacket?
    fun get(key: UShort): ControlPacket?
    fun remove(key: UShort): ControlPacket?
    fun clear()
    fun close()
}
