package mqtt.buffer

interface PlatformBuffer : ReadBuffer, WriteBuffer, SuspendCloseable {
    val type: BufferType
    fun clear()
    fun limit(newLimit: Int)
    fun put(buffer: PlatformBuffer)
    fun flip()
}