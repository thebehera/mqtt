package mqtt.wire.control.packet

import kotlinx.io.core.ByteReadPacket

interface DeserializationStrategy<T> {
    fun deserialize(buffer: ByteReadPacket): T
}

interface SerializationStrategy<T> {
    fun serialize(obj: T): ByteReadPacket
}

interface SerializableStrategy<T> : SerializationStrategy<T>, DeserializationStrategy<T>


data class SerializablePayload<T : Any>(
    val payload: T,
    val serializableStrategy: SerializationStrategy<T>,
    val size: UInt = serializableStrategy.serialize(payload).remaining.toUInt()
)

