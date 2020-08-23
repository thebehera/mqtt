@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.buffer

import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class ComposablePlatformBufferTests {

    @Test
    fun readByteFromFirstBuffer() {
        val expectedFirstByte = Byte.MAX_VALUE
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondByte = Byte.MIN_VALUE
        second.write(expectedSecondByte)
        second.resetForRead()

        val composableBuffer = ComposablePlatformBuffer(first, second)
        assertEquals(expectedFirstByte, composableBuffer.readByte())
    }

    @Test
    fun readByteFromSecondBuffer() {
        val expectedFirstByte = Byte.MAX_VALUE
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondByte = Byte.MIN_VALUE
        second.write(expectedSecondByte)
        second.resetForRead()

        val composableBuffer = ComposablePlatformBuffer(first, second)
        composableBuffer.position(1)
        assertEquals(expectedSecondByte, composableBuffer.readByte())
    }

    @Test
    fun readBytesFromThreeBuffers() {
        val expectedFirstByte = Byte.MAX_VALUE
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondByte = 6.toByte()
        second.write(expectedSecondByte)
        second.resetForRead()
        val third = allocateNewBuffer(1u)
        val expectedThirdByte = Byte.MIN_VALUE
        third.write(expectedThirdByte)
        third.resetForRead()

        val composableBuffer = arrayListOf(first, second, third).toComposableBuffer()
        assertEquals(expectedFirstByte, composableBuffer.readByte())
        assertEquals(expectedSecondByte, composableBuffer.readByte())
        assertEquals(expectedThirdByte, composableBuffer.readByte())
    }

    @Test
    fun readBytesFromFourBuffers() {
        val expectedFirstByte = Byte.MAX_VALUE
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondByte = 6.toByte()
        second.write(expectedSecondByte)
        second.resetForRead()
        val third = allocateNewBuffer(1u)
        val expectedThirdByte = 12.toByte()
        third.write(expectedThirdByte)
        third.resetForRead()

        val fourth = allocateNewBuffer(1u)
        val expectedFourthByte = Byte.MIN_VALUE
        fourth.write(expectedFourthByte)
        fourth.resetForRead()

        val composableBuffer = arrayListOf(first, second, third, fourth).toComposableBuffer()
        assertEquals(expectedFirstByte, composableBuffer.readByte())
        assertEquals(expectedSecondByte, composableBuffer.readByte())
        assertEquals(expectedThirdByte, composableBuffer.readByte())
        assertEquals(expectedFourthByte, composableBuffer.readByte())
    }
}