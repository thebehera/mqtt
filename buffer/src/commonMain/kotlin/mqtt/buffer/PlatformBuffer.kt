package mqtt.buffer

interface PlatformBuffer : ReadBuffer, WriteBuffer, SuspendCloseable {
    val type: BufferType
    fun clear()
    fun put(buffer: PlatformBuffer)
}