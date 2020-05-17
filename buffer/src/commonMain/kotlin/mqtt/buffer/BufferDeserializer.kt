@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.buffer

interface BufferDeserializer<T : Any> {
    fun deserialize(
        buffer: ReadBuffer,
        length: UShort = 0u,
        path: CharSequence? = null,
        headers: Map<CharSequence, Set<CharSequence>>? = null
    ): GenericType<T>?
}