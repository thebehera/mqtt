package mqtt.buffer

interface PlatformBuffer : ReadBuffer, WriteBuffer {
    val type: BufferType
    fun clear()
    fun limit(newLimit: Int)
    fun put(buffer: PlatformBuffer)
    fun flip()
    suspend fun close()
}