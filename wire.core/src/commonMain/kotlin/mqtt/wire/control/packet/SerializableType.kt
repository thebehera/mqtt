package mqtt.wire.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully

interface MqttSerializationStrategy<T> {
    fun serialize(obj: T) :ByteReadPacket
}

interface MqttDeserializationStrategy<T> {
    fun deserialize(buffer: ByteReadPacket) :T
}


interface MqttSerializable<T> : MqttSerializationStrategy<T>, MqttDeserializationStrategy<T>


object ByteArraySerializer :MqttSerializable<ByteArray> {
    override fun serialize(obj: ByteArray) = buildPacket { writeFully(obj) }
    override fun deserialize(buffer: ByteReadPacket) = buffer.readBytes()
}
