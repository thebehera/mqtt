@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.buffer

data class BufferPool(val limits: BufferMemoryLimit) {
    val inMemoryPool = LinkedHashSet<PlatformBuffer>()

    fun borrow(size: UInt = limits.defaultBufferSize) = allocateNewBuffer(size, limits)

    fun borrow(size: UInt = limits.defaultBufferSize, cb: ((PlatformBuffer) -> Unit)) {
        val buffer = borrow(size)
        cb(buffer)
        recycle(buffer)
    }

    suspend fun <T> borrowSuspend(size: UInt = limits.defaultBufferSize, cb: suspend ((PlatformBuffer) -> T)): T {
        val buffer = borrow(size)
        buffer.resetForWrite()
        try {
            return cb(buffer)
        } finally {
            recycle(buffer)
        }
    }

    fun recycle(buffer: PlatformBuffer) {
        if (buffer.type == BufferType.InMemory) {
            inMemoryPool += buffer
        }
        // TODO finish impl
    }
}