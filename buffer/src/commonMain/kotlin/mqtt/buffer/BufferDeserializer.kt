@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.buffer

interface BufferDeserializer<T : Any> {
    fun deserialize(buffer: ReadBuffer): T
}