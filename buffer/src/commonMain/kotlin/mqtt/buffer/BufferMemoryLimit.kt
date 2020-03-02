package mqtt.buffer

interface BufferMemoryLimit {
    val tmpBufferPrefix: String get() = "mqttTmp"
    val defaultBufferSize: UInt get() = 6.toUInt()
    fun isTooLargeForMemory(size: UInt): Boolean
}