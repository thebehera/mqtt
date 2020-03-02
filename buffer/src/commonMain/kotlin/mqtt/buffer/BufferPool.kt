package mqtt.buffer

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
    }
}