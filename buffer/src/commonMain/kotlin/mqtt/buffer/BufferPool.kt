@file:Suppress("EXPERIMENTAL_API_USAGE", "DANGEROUS_INTERNAL_IO_API")

package mqtt.buffer

data class BufferPool(val limits: BufferMemoryLimit = DefaultMemoryLimit) {
    internal val pool = HashSet<PlatformBuffer>()

    fun borrow(size: UInt = limits.defaultBufferSize, cb: ((PlatformBuffer) -> Unit)) {
        val buffer = borrow(size)
        try {
            cb(buffer)
        } finally {
            recycle(buffer)
        }
    }

    fun borrowAsync(
        size: UInt = limits.defaultBufferSize,
        cb: (PlatformBuffer, RecycleCallback) -> Unit
    ) {
        val buffer = borrow(size)
        cb(buffer, RecycleCallbackImpl(this, buffer))
    }

    suspend fun <T> borrowSuspend(size: UInt = limits.defaultBufferSize, cb: suspend ((PlatformBuffer) -> T)): T {
        val buffer = borrow(size)
        try {
            return cb(buffer)
        } finally {
            recycle(buffer)
        }
    }

    fun releaseAllBuffers() {
        pool.clear()
    }

    private fun borrow(size: UInt = limits.defaultBufferSize) = pool
        .sortedBy { it.capacity }
        .minBy {
            if (it.capacity.toLong() < size.toLong()) {
                Int.MAX_VALUE.toLong()
            } else {
                it.capacity.toLong()
            }
        } ?: allocateNewBuffer(size, limits)

    private fun recycle(buffer: PlatformBuffer) {
        if (buffer.type == BufferType.InMemory) {
            buffer.resetForWrite()
            pool += buffer
        }
    }

    interface RecycleCallback {
        fun recycle()
    }

    private class RecycleCallbackImpl(val pool: BufferPool, val buffer: PlatformBuffer) : RecycleCallback {
        override fun recycle() {
            pool.recycle(buffer)
        }
    }

}