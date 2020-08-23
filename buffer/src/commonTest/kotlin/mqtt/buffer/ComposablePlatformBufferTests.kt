@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.buffer

import kotlin.test.Test
import kotlin.test.assertEquals


@ExperimentalUnsignedTypes
class ComposablePlatformBufferTests {

    @Test
    fun readByteFromFirstBuffer() {
        val expectedFirstByte = 5.toByte()
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondByte = 6.toByte()
        second.write(expectedSecondByte)
        second.resetForRead()

        val composableBuffer = ComposablePlatformBuffer(first, second)
        assertEquals(expectedFirstByte, composableBuffer.readByte())
    }

    @Test
    fun readByteFromSecondBuffer() {
        val expectedFirstByte = 5.toByte()
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondByte = 6.toByte()
        second.write(expectedSecondByte)
        second.resetForRead()

        val composableBuffer = ComposablePlatformBuffer(first, second)
        composableBuffer.position(1)
        assertEquals(expectedSecondByte, composableBuffer.readByte())
    }

}