package mqtt.buffer

interface PlatformBuffer : ReadBuffer, WriteBuffer, SuspendCloseable {
    val type: BufferType
    fun put(buffer: PlatformBuffer)
}