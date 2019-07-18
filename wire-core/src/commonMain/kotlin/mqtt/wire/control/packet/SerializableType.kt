package mqtt.wire.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import kotlin.reflect.KClass

interface MqttSerializationStrategy<T> {
    fun serialize(obj: T): ByteReadPacket
}

interface MqttDeserializationStrategy<T> {
    fun deserialize(buffer: ByteReadPacket): T
}


interface MqttSerializable<T : Any> : MqttSerializationStrategy<T>, MqttDeserializationStrategy<T>

object ByteArraySerializer : MqttSerializable<ByteArray> {
    override fun serialize(obj: ByteArray) = buildPacket { writeFully(obj) }
    override fun deserialize(buffer: ByteReadPacket) = buffer.readBytes()
}

object StringSerializer : MqttSerializable<String> {
    override fun serialize(obj: String) = buildPacket { writeStringUtf8(obj) }
    override fun deserialize(buffer: ByteReadPacket) = buffer.readText()
}

val serializers = mutableMapOf<KClass<*>, MqttSerializable<*>>(
    Pair(ByteArray::class, ByteArraySerializer),
    Pair(String::class, StringSerializer)
)

inline fun <reified T : Any> findSerializer(): MqttSerializable<T>? {
    @Suppress("UNCHECKED_CAST")
    return serializers[T::class] as? MqttSerializable<T>?
}

fun <T : Any> findSerializer(kClass: KClass<T>): MqttSerializable<T>? {
    @Suppress("UNCHECKED_CAST")
    return serializers[kClass] as? MqttSerializable<T>?
}

inline fun <reified T : Any> installSerializer(serializable: MqttSerializable<T>) {
    serializers[T::class] = serializable
}
