@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.buffer

interface BufferDeserializer<T : Any> {
    fun deserialize(
        buffer: ReadBuffer,
        path: CharSequence? = null,
        headers: List<Pair<CharSequence, CharSequence>>? = null
    ): T
}