@file:Suppress("EXPERIMENTAL_API_USAGE", "DANGEROUS_INTERNAL_IO_API")

package mqtt.buffer

/**
 * The goal of the buffer pool is to provide a performance increase at the expense of security complexity.
 * When using a pool, it is not required to "zero"-out a buffer compared to allocating/de-allocating which has that
 * overhead. It is highly suggested to ensure that the buffer pool is allocated per socket or user connection to
 * prevent accidentally leaking information to the wrong place
 */
data class BufferPool(val limits: BufferMemoryLimit = DefaultMemoryLimit) {
    internal val pool = HashSet<PlatformBuffer>()

    fun borrow(size: UInt = limits.defaultBufferSize, bufferCallback: ((PlatformBuffer) -> Unit)) {
        val buffer = borrow(size)
        try {
            bufferCallback(buffer)
        } finally {
            recycle(buffer)
        }
    }

    /**
     * Take care when calling this function and be sure to call RecycleCallback#recycle to ensure the buffer is cleaned
     * up, otherwise cannot be reused (leading to more expensive allocations and "zero-ing" out of buffers)
     */
    fun borrowAsync(
        size: UInt = limits.defaultBufferSize,
        bufferCallback: (PlatformBuffer, RecycleCallback) -> Unit
    ) {
        val buffer = borrow(size)
        bufferCallback(buffer, RecycleCallbackImpl(this, buffer))
    }

    suspend fun <T> borrowSuspend(
        size: UInt = limits.defaultBufferSize,
        bufferCallback: suspend ((PlatformBuffer) -> T)
    ): T {
        val buffer = borrow(size)
        try {
            return bufferCallback(buffer)
        } finally {
            recycle(buffer)
        }
    }

    /**
     * Release all references to all buffers in the pool
     */
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