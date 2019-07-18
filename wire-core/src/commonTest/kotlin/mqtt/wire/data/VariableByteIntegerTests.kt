@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.data

import kotlinx.io.core.readBytes
import mqtt.wire.MalformedInvalidVariableByteInteger
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class VariableByteIntegerTests {

    @Test
    @JsName("encodedValueMustUseMinNumberOfBytes")
    fun `MQTT Conformance The encoded value MUST use the minimum number of bytes necessary to represent the value`() {
        val oneMin = VariableByteInteger(0.toUInt())
        assertEquals(1, oneMin.encodedValue().remaining.toInt())
        val oneMax = VariableByteInteger(127.toUInt())
        assertEquals(1, oneMax.encodedValue().remaining.toInt())
        val twoMin = VariableByteInteger(128.toUInt())
        assertEquals(2, twoMin.encodedValue().remaining.toInt())
        val twoMax = VariableByteInteger(16_383.toUInt())
        assertEquals(2, twoMax.encodedValue().remaining.toInt())
        val threeMin = VariableByteInteger(16_384.toUInt())
        assertEquals(3, threeMin.encodedValue().remaining.toInt())
        val threeMax = VariableByteInteger(2_097_151.toUInt())
        assertEquals(3, threeMax.encodedValue().remaining.toInt())
        val fourMin = VariableByteInteger(2_097_152.toUInt())
        assertEquals(4, fourMin.encodedValue().remaining.toInt())
        val fourMax = VariableByteInteger(268_435_455.toUInt())
        assertEquals(4, fourMax.encodedValue().remaining.toInt())
    }

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
        assertEquals(1, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger().toInt()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles1() {
        val expectedValue = 1
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
        assertEquals(1, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger().toInt()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles127() {
        val expectedValue = 127.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
        assertEquals(1, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles128() {
        val expectedValue = 128.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
        assertEquals(2, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles16383() {
        val expectedValue = 16383.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
        assertEquals(2, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles16384() {
        val expectedValue = 16384.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
        assertEquals(3, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handles65535() {
        val expectedValue = 65535.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
        assertEquals(3, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handlesMaxMinus1() {
        val expectedValue = VARIABLE_BYTE_INT_MAX - 1.toUInt()
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
        assertEquals(4, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun handlesMax() {
        val expectedValue = VARIABLE_BYTE_INT_MAX
        val variableIntBytes = VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
        assertEquals(4, variableIntBytes.size, "Incorrect byte array size")
        val actual = variableIntBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }


    @Test
    fun handlesMaxPlus1() {
        val expectedValue = VARIABLE_BYTE_INT_MAX + 1.toUInt()
        try {
            VariableByteInteger(expectedValue.toUInt()).encodedValue().readBytes()
            fail("should of thrown an error")
        } catch (e: MalformedInvalidVariableByteInteger) {
        }
    }
}
