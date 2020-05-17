@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.buffer

interface BufferSerializer<T : Any> {
    fun size(buffer: WriteBuffer, obj: T): UInt
    fun serialize(buffer: WriteBuffer, obj: T)
}

