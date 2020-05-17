@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.buffer

import kotlin.random.Random
import kotlin.random.nextUInt
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
        val byte = Random.nextBytes(1).first()
        platformBuffer.write(byte)
        platformBuffer.resetForRead()
        assertEquals(byte, platformBuffer.readByte())
    }

    @Test
    fun byteArray() {
        val size = 200
        val platformBuffer = allocateNewBuffer(size.toUInt(), limit)
        val bytes = Random.nextBytes(size)
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
        val byte = Random.nextBytes(1).first().toUByte()
        platformBuffer.write(byte)
        platformBuffer.resetForRead()
        assertEquals(byte, platformBuffer.readUnsignedByte())
    }

    @Test
    fun unsignedShort() {
        val platformBuffer = allocateNewBuffer(2u, limit)
        val uShort = Random.nextInt().toUShort()
        platformBuffer.write(uShort)
        platformBuffer.resetForRead()
        assertEquals(uShort, platformBuffer.readUnsignedShort())
    }

    @Test
    fun unsignedInt() {
        val platformBuffer = allocateNewBuffer(4u, limit)
        val uInt = Random.nextUInt()
        platformBuffer.write(uInt)
        platformBuffer.resetForRead()
        assertEquals(uInt, platformBuffer.readUnsignedInt())
    }

    @Test
    fun long() {
        val platformBuffer = allocateNewBuffer(8u, limit)
        val long = Random.nextLong()
        platformBuffer.write(long)
        platformBuffer.resetForRead()
        assertEquals(long, platformBuffer.readLong())
    }

    @Test
    @ExperimentalStdlibApi
    fun utf8String() {
        val string = "yolo swag lyfestyle"
        val platformBuffer = allocateNewBuffer(21u, limit)
        platformBuffer.writeMqttUtf8String(string)
        platformBuffer.resetForRead()
        assertEquals(string, platformBuffer.readMqttUtf8StringNotValidated().toString())
    }
}