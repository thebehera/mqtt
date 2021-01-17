@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire.data

import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.allocateNewBuffer
import mqtt.wire.buffer.MalformedInvalidVariableByteInteger
import mqtt.wire.buffer.readVariableByteInteger
import mqtt.wire.buffer.variableByteSize
import mqtt.wire.buffer.writeVariableByteInteger
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
        assertEquals(1u, variableByteSize(oneMin))
        val oneMax = 127u
        assertEquals(1u, variableByteSize(oneMax))
        val twoMin = 128u
        assertEquals(2u, variableByteSize(twoMin))
        val twoMax = 16_383u
        assertEquals(2u, variableByteSize(twoMax))
        val threeMin = 16_384u
        assertEquals(3u, variableByteSize(threeMin))
        val threeMax = 2_097_151u
        assertEquals(3u, variableByteSize(threeMax))
        val fourMin = 2_097_152u
        assertEquals(4u, variableByteSize(fourMin))
        val fourMax = 268_435_455u
        assertEquals(4u, variableByteSize(fourMax))
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
