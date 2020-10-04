@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.buffer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalUnsignedTypes
class FragmentedReadBufferTests {

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

        val composableBuffer = FragmentedReadBuffer(first, second)
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

        val composableBuffer = FragmentedReadBuffer(first, second)
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

        val composableBuffer = FragmentedReadBuffer(first, second)
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

        val composableBuffer = FragmentedReadBuffer(first, second)
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

    @Test
    fun readUnsignedShortFromFirstBuffer() {
        val expectedFirstUShort = UShort.MAX_VALUE
        val first = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        first.write(expectedFirstUShort)
        first.resetForRead()

        val second = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        val expectedSecondUShort = UShort.MIN_VALUE
        second.write(expectedSecondUShort)
        second.resetForRead()

        val composableBuffer = FragmentedReadBuffer(first, second)
        assertEquals(expectedFirstUShort, composableBuffer.readUnsignedShort())
        assertEquals(expectedSecondUShort, composableBuffer.readUnsignedShort())
    }

    @Test
    fun readUnsignedShortFromSecondBuffer() {
        val expectedFirstUShort = UShort.MAX_VALUE
        val first = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        first.write(expectedFirstUShort)
        first.resetForRead()
        val second = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        val expectedSecondUShort = UShort.MIN_VALUE
        second.write(expectedSecondUShort)
        second.resetForRead()

        val composableBuffer = FragmentedReadBuffer(first, second)
        composableBuffer.position(UShort.SIZE_BYTES)
        assertEquals(expectedSecondUShort, composableBuffer.readUnsignedShort())
    }

    @Test
    fun readUnsignedShortsFromThreeBuffers() {
        val expectedFirstUShort = UShort.MAX_VALUE
        val first = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        first.write(expectedFirstUShort)
        first.resetForRead()
        val second = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        val expectedSecondUShort = 6.toUShort()
        second.write(expectedSecondUShort)
        second.resetForRead()
        val third = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        val expectedThirdUShort = UShort.MIN_VALUE
        third.write(expectedThirdUShort)
        third.resetForRead()

        val composableBuffer = arrayListOf(first, second, third).toComposableBuffer()
        assertEquals(expectedFirstUShort, composableBuffer.readUnsignedShort())
        assertEquals(expectedSecondUShort, composableBuffer.readUnsignedShort())
        assertEquals(expectedThirdUShort, composableBuffer.readUnsignedShort())
    }

    @Test
    fun readUnsignedShortsFromFourBuffers() {
        val expectedFirstUShort = UShort.MAX_VALUE
        val first = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        first.write(expectedFirstUShort)
        first.resetForRead()
        val second = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        val expectedSecondUShort = 6.toUShort()
        second.write(expectedSecondUShort)
        second.resetForRead()
        val third = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        val expectedThirdUShort = 12.toUShort()
        third.write(expectedThirdUShort)
        third.resetForRead()

        val fourth = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        val expectedFourthUShort = UShort.MIN_VALUE
        fourth.write(expectedFourthUShort)
        fourth.resetForRead()

        val composableBuffer = arrayListOf(first, second, third, fourth).toComposableBuffer()
        assertEquals(expectedFirstUShort, composableBuffer.readUnsignedShort())
        assertEquals(expectedSecondUShort, composableBuffer.readUnsignedShort())
        assertEquals(expectedThirdUShort, composableBuffer.readUnsignedShort())
        assertEquals(expectedFourthUShort, composableBuffer.readUnsignedShort())
    }

    @Test
    fun readUnsignedShortsFromFiveBuffers() {
        val expectedFirstUShort = UShort.MAX_VALUE
        val first = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        first.write(expectedFirstUShort)
        first.resetForRead()
        val second = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        val expectedSecondUShort = 6.toUShort()
        second.write(expectedSecondUShort)
        second.resetForRead()
        val third = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        val expectedThirdUShort = (-1).toUShort()
        third.write(expectedThirdUShort)
        third.resetForRead()

        val fourth = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        val expectedFourthUShort = 0.toUShort()
        fourth.write(expectedFourthUShort)
        fourth.resetForRead()

        val fifth = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        val expectedFifthUShort = UShort.MIN_VALUE
        fifth.write(expectedFifthUShort)
        fifth.resetForRead()

        val composableBuffer = arrayListOf(first, second, third, fourth, fifth).toComposableBuffer()
        assertEquals(expectedFirstUShort, composableBuffer.readUnsignedShort())
        assertEquals(expectedSecondUShort, composableBuffer.readUnsignedShort())
        assertEquals(expectedThirdUShort, composableBuffer.readUnsignedShort())
        assertEquals(expectedFourthUShort, composableBuffer.readUnsignedShort())
        assertEquals(expectedFifthUShort, composableBuffer.readUnsignedShort())
    }

    @Test
    fun readFragmentedStringFromThreeBuffers() {
        val expectedString = "yolo-swag-lyfestyle"
        val utf8length = expectedString.toBuffer().limit()
        val composableBuffer = expectedString
            .split(Regex("(?=-)"))
            .map { it.toBuffer() }
            .toComposableBuffer()
        val actual = composableBuffer.readUtf8(utf8length)
        assertEquals(expectedString, actual.toString())
    }

    @Test
    fun utf8Line() {
        val buffers = arrayOf("yolo\r\n", "\nsw\n\r\nag", "\r\nli\n\r\nfe\r\nstyle\r\n")
        val composableBuffer = buffers.map { it.toBuffer() }.toComposableBuffer()
        assertEquals("yolo", composableBuffer.readUtf8Line().toString())
        assertEquals("", composableBuffer.readUtf8Line().toString())
        assertEquals("sw", composableBuffer.readUtf8Line().toString())
        assertEquals("", composableBuffer.readUtf8Line().toString())
        assertEquals("ag", composableBuffer.readUtf8Line().toString())
        assertEquals("li", composableBuffer.readUtf8Line().toString())
        assertEquals("", composableBuffer.readUtf8Line().toString())
        assertEquals("fe", composableBuffer.readUtf8Line().toString())
        assertEquals("style", composableBuffer.readUtf8Line().toString())
        assertEquals("", composableBuffer.readUtf8Line().toString())
        assertTrue { composableBuffer.remaining() == 0u }
    }
}