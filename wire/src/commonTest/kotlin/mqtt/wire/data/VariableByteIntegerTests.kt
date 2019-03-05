@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.data

import kotlinx.io.core.readBytes
import mqtt.wire.MalformedInvalidVariableByteInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class VariableByteIntegerTests {

    @Test
    fun encodeAndDecode() {
        val numberToEncode = 268435442.toUInt()
        val encodedNumber = VariableByteInteger(numberToEncode.toUInt()).encodedValue()
        val decoded = encodedNumber.decodeVariableByteInteger()
        assertEquals(numberToEncode, decoded)
    }

    @Test
    fun oversize() {
        try {
            VariableByteInteger(Int.MAX_VALUE.toUInt()).encodedValue()
            fail("Should of have hit an exception")
        } catch (e: MalformedInvalidVariableByteInteger) {
        }
    }


    @Test
    fun handlesNegative1() {
        val variableByteInteger = VariableByteInteger((-1).toUInt())
        try {
            variableByteInteger.encodedValue()
            fail("should of thrown an exception")
        } catch (e: MalformedInvalidVariableByteInteger) {
        }
    }

    @Test
    fun handles0() {
        val expectedValue = 0
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(1, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger().toInt()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles1() {
        val expectedValue = 1
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(1, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger().toInt()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles127() {
        val expectedValue = 127.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(1, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles128() {
        val expectedValue = 128.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(2, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles16383() {
        val expectedValue = 16383.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(2, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles16384() {
        val expectedValue = 16384.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(3, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles65535() {
        val expectedValue = 65535.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(3, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handlesMaxMinus1() {
        val expectedValue = VARIABLE_BYTE_INT_MAX - 1.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(4, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handlesMax() {
        val expectedValue = VARIABLE_BYTE_INT_MAX
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(4, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }


    @Test
    fun handlesMaxPlus1() {
        val expectedValue = VARIABLE_BYTE_INT_MAX + 1.toUInt()
        try {
            VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
                    .drop(1) // drop the first byte since we dont care about the control packet type
                    .toByteArray()
            fail("should of thrown an error")
        } catch (e: MalformedInvalidVariableByteInteger) {
        }
    }
}
