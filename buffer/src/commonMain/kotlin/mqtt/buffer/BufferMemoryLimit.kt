@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package mqtt.buffer

interface BufferMemoryLimit {
    val tmpBufferPrefix: String get() = "mqttTmp"
    val defaultBufferSize: UInt get() = 4096u
    fun isTooLargeForMemory(size: UInt): Boolean
}

object DefaultMemoryLimit : BufferMemoryLimit {
    override fun isTooLargeForMemory(size: UInt) = size > defaultBufferSize
}

object UnlimitedMemoryLimit : BufferMemoryLimit {
    override fun isTooLargeForMemory(size: UInt) = false
}