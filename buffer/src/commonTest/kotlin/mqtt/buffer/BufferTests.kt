@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.buffer

import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class BufferTests {
    val limit = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    @Test
    fun byte() {
        val platformBuffer = allocateNewBuffer(1u, limit)
        val byte = (-1).toByte()
        platformBuffer.write(byte)
        platformBuffer.resetForRead()
        assertEquals(byte.toInt(), platformBuffer.readByte().toInt())
    }

    @Test
    fun byteArray() {
        val size = 200
        val platformBuffer = allocateNewBuffer(size.toUInt(), limit)
        val bytes = ByteArray(200) { -1 }
        platformBuffer.write(bytes)
        platformBuffer.resetForRead()
        val byteArray = platformBuffer.readByteArray(size.toUInt())
        assertEquals(bytes.count(), byteArray.count())
        var count = 0
        for (byte in bytes) {
            assertEquals(byte, byteArray[count++])
        }
    }

    @Test
    fun unsignedByte() {
        val platformBuffer = allocateNewBuffer(1u, limit)
        val byte = (-1).toUByte()
        platformBuffer.write(byte)
        platformBuffer.resetForRead()
        assertEquals(byte.toInt(), platformBuffer.readUnsignedByte().toInt())
    }

    @Test
    fun unsignedShort() {
        val platformBuffer = allocateNewBuffer(2u, limit)
        val uShort = (-1).toUShort()
        platformBuffer.write(uShort)
        platformBuffer.resetForRead()
        assertEquals(uShort.toInt(), platformBuffer.readUnsignedShort().toInt())
    }

    @Test
    fun unsignedInt() {
        val platformBuffer = allocateNewBuffer(4u, limit)
        val uInt = (-1).toUInt()
        platformBuffer.write(uInt)
        platformBuffer.resetForRead()
        assertEquals(uInt.toLong(), platformBuffer.readUnsignedInt().toLong())
    }

    @Test
    fun long() {
        val platformBuffer = allocateNewBuffer(Long.SIZE_BYTES.toUInt(), limit)
        val long = (-1).toLong()
        platformBuffer.write(long)
        platformBuffer.resetForRead()
        assertEquals(long, platformBuffer.readLong())
    }

    @Test
    @ExperimentalStdlibApi
    fun mqttUtf8String() {
        val string = "yolo swag lyfestyle"
        val tmpBuffer = allocateNewBuffer(1u)
        assertEquals(19, tmpBuffer.sizeUtf8String(string).toInt())
        val platformBuffer = allocateNewBuffer(21u, limit)
        platformBuffer.writeMqttUtf8String(string)
        platformBuffer.resetForRead()
        val actual = platformBuffer.readMqttUtf8StringNotValidated().toString()
        assertEquals(string.length, actual.length)
        assertEquals(string, actual)
    }

    @Test
    @ExperimentalStdlibApi
    fun utf8String() {
        val string = "yolo swag lyfestyle"
        val tmpBuffer = allocateNewBuffer(1u)
        assertEquals(19, tmpBuffer.sizeUtf8String(string).toInt())
        val platformBuffer = allocateNewBuffer(19u, limit)
        platformBuffer.writeUtf8(string)
        platformBuffer.resetForRead()
        val actual = platformBuffer.readUtf8(19u).toString()
        assertEquals(string.length, actual.length)
        assertEquals(string, actual)
    }
}