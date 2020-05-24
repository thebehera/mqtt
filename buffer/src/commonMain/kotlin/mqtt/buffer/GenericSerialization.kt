@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.buffer

import kotlin.reflect.KClass

object GenericSerialization {
    val serializerMap = SketchyBufferSerializerMap().also {
        it[CharSequence::class] = CharSequenceSerializer
        it[String::class] = CharSequenceSerializer
    }
    val deserializerMap = SketchyBufferDeserializerMap().also {
        it[CharSequence::class] = CharSequenceSerializer
        it[String::class] = CharSequenceSerializer
    }

    fun <T : Any> registerSerializer(serializer: BufferSerializer<T>) {
        serializerMap[serializer.kClass] = serializer
    }

    fun <T : Any> serialize(buffer: WriteBuffer, genericType: GenericType<T>) {
        if (genericType.kClass == Unit::class) {
            return
        }
        serializerMap.get<T>(genericType.kClass).serialize(buffer, genericType.obj)
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

    fun deserialize(deserializationParameters: DeserializationParameters): Any? {
        //TODO: Finish this topic matching to the deserializer
//        return deserializerMap.get<T>(type).deserialize(readBuffer, length, path, headers)?.obj
        return null
    }
}

object CharSequenceSerializer : BufferSerializer<CharSequence>, BufferDeserializer<CharSequence> {
    override val kClass: KClass<CharSequence> = CharSequence::class
    override fun size(buffer: WriteBuffer, obj: CharSequence) = buffer.lengthUtf8String(obj)

    override fun serialize(buffer: WriteBuffer, obj: CharSequence): Boolean {
        buffer.writeUtf8(obj)
        return true
    }

    override fun deserialize(parameters: DeserializationParameters): GenericType<CharSequence>? {
        val obj = parameters.buffer.readUtf8(parameters.length.toUInt())
        return GenericType(obj, CharSequence::class)
    }
}