@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.buffer

import kotlin.reflect.KClass

object GenericSerialization {
    val serializerMap = SketchyBufferSerializerMap()
    val deserializerMap = SketchyBufferDeserializerMap()

    inline fun <reified T : Any> registerSerializer(serializer: BufferSerializer<T>) {
        serializerMap[T::class] = serializer
    }

    fun <T : Any> serialize(buffer: WriteBuffer, obj: T, type: KClass<T>) {
        if (type == Unit::class) {
            return
        }
        serializerMap.get<T>(type).serialize(buffer, obj)
    }

    fun <T : Any> size(buffer: WriteBuffer, obj: T, type: KClass<T>): UInt {
        if (type == Unit::class) {
            return 0u
        }
        return serializerMap.get<T>(type).size(buffer, obj)
    }

    inline fun <reified T : Any> registerDeserializer(deserializer: BufferDeserializer<T>) {
        deserializerMap[T::class] = deserializer
    }

    fun <T : Any> deserialize(type: KClass<T>, readBuffer: ReadBuffer): T? {
        if (type == Unit::class) {
            return null
        }
        return deserializerMap.get<T>(type).deserialize(readBuffer)
    }
}