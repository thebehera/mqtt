package mqtt.buffer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BufferPoolTests {

    @Test
    fun recycles() {
        val bufferPool = BufferPool()
        var buffer: PlatformBuffer? = null
        bufferPool.borrow {
            buffer = it
        }
        assertTrue(bufferPool.pool.contains(buffer!!))
        bufferPool.borrow {
            assertEquals(buffer!!, it)
        }
    }

    @Test
    fun recyclesAsync() {
        val bufferPool = BufferPool()
        var buffer: PlatformBuffer? = null
        bufferPool.borrowAsync { borredBuffer, cb ->
            buffer = borredBuffer
            cb.recycle()
        }
        assertTrue(bufferPool.pool.contains(buffer!!))
        bufferPool.borrow {
            assertEquals(buffer!!, it)
        }
    }


    @Test
    fun clearsPool() {
        val bufferPool = BufferPool()
        bufferPool.borrowAsync { borredBuffer, cb ->
            bufferAsyc = borredBuffer
            cb.recycle()
        }
        bufferPool.borrow {
            buffer = it
        }
        bufferPool.releaseAllBuffers()
        assertTrue(bufferPool.pool.isEmpty())
    }
}