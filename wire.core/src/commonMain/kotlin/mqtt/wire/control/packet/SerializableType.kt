package mqtt.wire.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import kotlin.reflect.KClass

interface MqttSerializationStrategy<T> {
    fun serialize(obj: T) :ByteReadPacket
}

interface MqttDeserializationStrategy<T> {
    fun deserialize(buffer: ByteReadPacket) :T
}


interface MqttSerializable<T:Any> : MqttSerializationStrategy<T>, MqttDeserializationStrategy<T> {
    val clazz: KClass<T>
}


object ByteArraySerializer :MqttSerializable<ByteArray> {
    override val clazz: KClass<ByteArray> = ByteArray::class
    override fun serialize(obj: ByteArray) = buildPacket { writeFully(obj) }
    override fun deserialize(buffer: ByteReadPacket) = buffer.readBytes()
}

object StringSerializer :MqttSerializable<String> {
    override val clazz: KClass<String> = String::class
    override fun serialize(obj: String) = buildPacket { writeStringUtf8(obj) }
    override fun deserialize(buffer: ByteReadPacket) = buffer.readText()
}

val serializers = setOf(
        ByteArraySerializer, StringSerializer
)

inline fun <reified T:Any> findSerializerOfType(): MqttSerializable<T>? {
    serializers.forEach {
        if (it.clazz == T::class) {
            @Suppress("UNCHECKED_CAST")
            return it as MqttSerializable<T>
        }
    }
    return null
}

fun main() {
    val serializer = findSerializerOfType<String>()

    serializer.toString()
}