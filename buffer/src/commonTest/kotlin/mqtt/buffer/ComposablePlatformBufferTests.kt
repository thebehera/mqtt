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

    @Test
    fun readBytesFromFiveBuffers() {
        val expectedFirstByte = Byte.MAX_VALUE
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondByte = 6.toByte()
        second.write(expectedSecondByte)
        second.resetForRead()
        val third = allocateNewBuffer(1u)
        val expectedThirdByte = (-1).toByte()
        third.write(expectedThirdByte)
        third.resetForRead()

        val fourth = allocateNewBuffer(1u)
        val expectedFourthByte = 0.toByte()
        fourth.write(expectedFourthByte)
        fourth.resetForRead()

        val fifth = allocateNewBuffer(1u)
        val expectedFifthByte = Byte.MIN_VALUE
        fifth.write(expectedFifthByte)
        fifth.resetForRead()

        val composableBuffer = arrayListOf(first, second, third, fourth, fifth).toComposableBuffer()
        assertEquals(expectedFirstByte, composableBuffer.readByte())
        assertEquals(expectedSecondByte, composableBuffer.readByte())
        assertEquals(expectedThirdByte, composableBuffer.readByte())
        assertEquals(expectedFourthByte, composableBuffer.readByte())
        assertEquals(expectedFifthByte, composableBuffer.readByte())
    }


    @Test
    fun readUByteFromFirstBuffer() {
        val expectedFirstUByte = UByte.MAX_VALUE
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstUByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondUByte = UByte.MIN_VALUE
        second.write(expectedSecondUByte)
        second.resetForRead()

        val composableBuffer = ComposablePlatformBuffer(first, second)
        assertEquals(expectedFirstUByte, composableBuffer.readUnsignedByte())
    }

    @Test
    fun readUByteeFromSecondBuffer() {
        val expectedFirstUByte = UByte.MAX_VALUE
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstUByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondUByte = UByte.MIN_VALUE
        second.write(expectedSecondUByte)
        second.resetForRead()

        val composableBuffer = ComposablePlatformBuffer(first, second)
        composableBuffer.position(1)
        assertEquals(expectedSecondUByte, composableBuffer.readUnsignedByte())
    }

    @Test
    fun readUByteFromThreeBuffers() {
        val expectedFirstUByte = UByte.MAX_VALUE
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstUByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondUByte = 6.toUByte()
        second.write(expectedSecondUByte)
        second.resetForRead()
        val third = allocateNewBuffer(1u)
        val expectedThirdUByte = UByte.MIN_VALUE
        third.write(expectedThirdUByte)
        third.resetForRead()

        val composableBuffer = arrayListOf(first, second, third).toComposableBuffer()
        assertEquals(expectedFirstUByte, composableBuffer.readUnsignedByte())
        assertEquals(expectedSecondUByte, composableBuffer.readUnsignedByte())
        assertEquals(expectedThirdUByte, composableBuffer.readUnsignedByte())
    }

    @Test
    fun readUByteFromFourBuffers() {
        val expectedFirstUByte = UByte.MAX_VALUE
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstUByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondUByte = 6.toUByte()
        second.write(expectedSecondUByte)
        second.resetForRead()
        val third = allocateNewBuffer(1u)
        val expectedThirdUByte = 12.toUByte()
        third.write(expectedThirdUByte)
        third.resetForRead()

        val fourth = allocateNewBuffer(1u)
        val expectedFourthUByte = UByte.MIN_VALUE
        fourth.write(expectedFourthUByte)
        fourth.resetForRead()

        val composableBuffer = arrayListOf(first, second, third, fourth).toComposableBuffer()
        assertEquals(expectedFirstUByte, composableBuffer.readUnsignedByte())
        assertEquals(expectedSecondUByte, composableBuffer.readUnsignedByte())
        assertEquals(expectedThirdUByte, composableBuffer.readUnsignedByte())
        assertEquals(expectedFourthUByte, composableBuffer.readUnsignedByte())
    }

    @Test
    fun readUByteFromFiveBuffers() {
        val expectedFirstUByte = UByte.MAX_VALUE
        val first = allocateNewBuffer(1u)
        first.write(expectedFirstUByte)
        first.resetForRead()
        val second = allocateNewBuffer(1u)
        val expectedSecondUByte = 6.toUByte()
        second.write(expectedSecondUByte)
        second.resetForRead()
        val third = allocateNewBuffer(1u)
        val expectedThirdUByte = (-1).toUByte()
        third.write(expectedThirdUByte)
        third.resetForRead()

        val fourth = allocateNewBuffer(1u)
        val expectedFourthUByte = 0.toUByte()
        fourth.write(expectedFourthUByte)
        fourth.resetForRead()

        val fifth = allocateNewBuffer(1u)
        val expectedFifthUByte = UByte.MIN_VALUE
        fifth.write(expectedFifthUByte)
        fifth.resetForRead()

        val composableBuffer = arrayListOf(first, second, third, fourth, fifth).toComposableBuffer()
        assertEquals(expectedFirstUByte, composableBuffer.readUnsignedByte())
        assertEquals(expectedSecondUByte, composableBuffer.readUnsignedByte())
        assertEquals(expectedThirdUByte, composableBuffer.readUnsignedByte())
        assertEquals(expectedFourthUByte, composableBuffer.readUnsignedByte())
        assertEquals(expectedFifthUByte, composableBuffer.readUnsignedByte())
    }

}