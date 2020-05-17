@file:Suppress("UNCHECKED_CAST")

package mqtt.buffer

import kotlin.reflect.KClass

class SketchyBufferSerializerMap {
    private val map = HashMap<KClass<*>, BufferSerializer<Any>>()

    fun <T : Any> get(kClass: KClass<*>): BufferSerializer<T> {
        return map[kClass]!! as BufferSerializer<T>
    }

    operator fun <T : Any> set(serializerType: KClass<T>, value: BufferSerializer<T>) {
        map[serializerType] = value as BufferSerializer<Any>
    }
}

class SketchyBufferDeserializerMap {
    private val map = HashMap<KClass<*>, BufferDeserializer<Any>>()

    fun <T : Any> get(kClass: KClass<*>): BufferDeserializer<T> {
        return map[kClass]!! as BufferDeserializer<T>
    }

    operator fun <T : Any> set(serializerType: KClass<T>, value: BufferDeserializer<T>) {
        map[serializerType] = value as BufferDeserializer<Any>
    }
}