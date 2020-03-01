package mqtt.socket

import mqtt.wire.data.ReadBuffer
import mqtt.wire.data.WriteBuffer


enum class BufferType {
    InMemory,
    Disk
}

interface PlatformBuffer : ReadBuffer, WriteBuffer, SuspendCloseable {
    val type: BufferType
    fun clear()
    fun limit(newLimit: Int)
    fun put(buffer: PlatformBuffer)
    fun flip()
}

interface BufferMemoryLimit {
    val tmpBufferPrefix: String get() = "mqttTmp"
    val defaultBufferSize: UInt get() = 6.toUInt()

    fun isTooLargeForMemory(size: UInt): Boolean
}

expect fun allocateNewBuffer(size: UInt, limits: BufferMemoryLimit): PlatformBuffer

data class BufferPool(val limits: BufferMemoryLimit) {
    val inMemoryPool = LinkedHashSet<PlatformBuffer>()

    fun borrow(size: UInt = limits.defaultBufferSize) = if (limits.isTooLargeForMemory(size)) {
        allocateNewBuffer(size, limits)
    } else {
        inMemoryPool.firstOrNull { it.limit().toUInt() >= size } ?: allocateNewBuffer(
            size,
            limits
        )
    }

    suspend fun recycle(buffer: PlatformBuffer) {
        if (buffer.type == BufferType.InMemory) {
            inMemoryPool += buffer
        }
        buffer.clear()
        buffer.close()
    }
}