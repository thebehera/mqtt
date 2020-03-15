package mqtt.buffer

data class BufferPool(val limits: BufferMemoryLimit) {
    val inMemoryPool = LinkedHashSet<PlatformBuffer>()

    fun borrow(size: UInt = limits.defaultBufferSize) = allocateNewBuffer(size, limits)

    suspend fun recycle(buffer: PlatformBuffer) {
        if (buffer.type == BufferType.InMemory) {
            inMemoryPool += buffer
        }
        // TODO finish impl
    }
}