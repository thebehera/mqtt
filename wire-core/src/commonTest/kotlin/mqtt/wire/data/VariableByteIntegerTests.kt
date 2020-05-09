@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire.data

import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.MalformedInvalidVariableByteInteger
import mqtt.buffer.allocateNewBuffer
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VariableByteIntegerTests {

    val VARIABLE_BYTE_INT_MAX = 268435455.toUInt()
    val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = false
    }

    @Test
    @JsName("encodedValueMustUseMinNumberOfBytes")
    fun `MQTT Conformance The encoded value MUST use the minimum number of bytes necessary to represent the value`() {
        val buffer = allocateNewBuffer(4u, limits)
        val oneMin = 0u
        assertEquals(1u, buffer.variableByteSize(oneMin))
        val oneMax = 127u
        assertEquals(1u, buffer.variableByteSize(oneMax))
        val twoMin = 128u
        assertEquals(2u, buffer.variableByteSize(twoMin))
        val twoMax = 16_383u
        assertEquals(2u, buffer.variableByteSize(twoMax))
        val threeMin = 16_384u
        assertEquals(3u, buffer.variableByteSize(threeMin))
        val threeMax = 2_097_151u
        assertEquals(3u, buffer.variableByteSize(threeMax))
        val fourMin = 2_097_152u
        assertEquals(4u, buffer.variableByteSize(fourMin))
        val fourMax = 268_435_455u
        assertEquals(4u, buffer.variableByteSize(fourMax))
    }

    @Test
    fun handles0() {
        val expectedValue = 0.toUInt()
        val buffer = allocateNewBuffer(1u, limits)
        buffer.writeVariableByteInteger(expectedValue.toUInt())
        buffer.resetForRead()
        assertEquals(expectedValue, buffer.readVariableByteInteger(), "Failed to read remaining bytes properly")
    }

    @Test
    fun handles1() {
        val expectedValue = 1.toUInt()
        val buffer = allocateNewBuffer(1u, limits)
        buffer.writeVariableByteInteger(expectedValue.toUInt())
        buffer.resetForRead()
        assertEquals(expectedValue, buffer.readVariableByteInteger(), "Failed to read remaining bytes properly")
    }

    @Test
    fun handles127() {
        val expectedValue = 127.toUInt()
        val buffer = allocateNewBuffer(1u, limits)
        buffer.writeVariableByteInteger(expectedValue.toUInt())
        buffer.resetForRead()
        assertEquals(expectedValue, buffer.readVariableByteInteger(), "Failed to read remaining bytes properly")
    }

    @Test
    fun handles128() {
        val expectedValue = 128.toUInt()
        val buffer = allocateNewBuffer(2u, limits)
        buffer.writeVariableByteInteger(expectedValue.toUInt())
        buffer.resetForRead()
        assertEquals(expectedValue, buffer.readVariableByteInteger(), "Failed to read remaining bytes properly")
    }

    @Test
    fun handles16383() {
        val expectedValue = 16383.toUInt()
        val buffer = allocateNewBuffer(2u, limits)
        buffer.writeVariableByteInteger(expectedValue.toUInt())
        buffer.resetForRead()
        assertEquals(expectedValue, buffer.readVariableByteInteger(), "Failed to read remaining bytes properly")
    }

    @Test
    fun handles16384() {
        val expectedValue = 16384.toUInt()
        val buffer = allocateNewBuffer(3u, limits)
        buffer.writeVariableByteInteger(expectedValue.toUInt())
        buffer.resetForRead()
        assertEquals(expectedValue, buffer.readVariableByteInteger(), "Failed to read remaining bytes properly")
    }

    @Test
    fun handles65535() {
        val expectedValue = 65535.toUInt()
        val buffer = allocateNewBuffer(3u, limits)
        buffer.writeVariableByteInteger(expectedValue.toUInt())
        buffer.resetForRead()
        assertEquals(expectedValue, buffer.readVariableByteInteger(), "Failed to read remaining bytes properly")
    }

    @Test
    fun handlesMaxMinus1() {
        val expectedValue = VARIABLE_BYTE_INT_MAX - 1.toUInt()
        val buffer = allocateNewBuffer(4u, limits)
        buffer.writeVariableByteInteger(expectedValue.toUInt())
        buffer.resetForRead()
        assertEquals(expectedValue, buffer.readVariableByteInteger(), "Failed to read remaining bytes properly")
    }

    @Test
    fun handlesMax() {
        val expectedValue = VARIABLE_BYTE_INT_MAX
        val buffer = allocateNewBuffer(4u, limits)
        buffer.writeVariableByteInteger(expectedValue.toUInt())
        buffer.resetForRead()
        assertEquals(expectedValue, buffer.readVariableByteInteger(), "Failed to read remaining bytes properly")
    }


    @Test
    fun handlesMaxPlus1() {
        val expectedValue = VARIABLE_BYTE_INT_MAX + 1.toUInt()
        val buffer = allocateNewBuffer(4u, limits)
        assertFailsWith(MalformedInvalidVariableByteInteger::class, "Larger than variable byte integer maximum") {
            buffer.writeVariableByteInteger(expectedValue.toUInt())
            buffer.resetForRead()
            buffer.readVariableByteInteger()
        }
    }
}
