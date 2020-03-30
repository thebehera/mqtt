@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.buffer

interface BufferMemoryLimit {
    val tmpBufferPrefix: String get() = "mqttTmp"
    val defaultBufferSize: UInt get() = 4096u
    fun isTooLargeForMemory(size: UInt): Boolean
}