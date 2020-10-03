@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.buffer

import kotlin.reflect.KClass

interface BufferSerializer<T : Any> {
    val kClass: KClass<T>
    fun size(obj: T): UInt
    fun serialize(buffer: WriteBuffer, obj: T): Boolean = true
}

