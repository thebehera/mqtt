package mqtt.wire.control.packet.fixed

import mqtt.wire.MalformedInvalidVariableByteInteger
import mqtt.wire.control.packet.fixed.ControlPacketType.*
import mqtt.wire.control.packet.fixed.FixedHeader.Companion.fromType
import mqtt.wire.data.VARIABLE_BYTE_INT_MAX
import mqtt.wire.data.decodeVariableByteInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class HeaderTests {
    @Test
    fun reserved() {
        val expected = RESERVED
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun connect() {
        val expected = CONNECT
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun connack() {
        val expected = CONNACK
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun publish() {
        val expected = PUBLISH
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun puback() {
        val expected = PUBACK
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun pubrec() {
        val expected = PUBREC
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun pubrel() {
        val expected = PUBREL
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun pubcomp() {
        val expected = PUBCOMP
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun subscribe() {
        val expected = SUBSCRIBE
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun suback() {
        val expected = SUBACK
        val fixedHeader = fromType(expected).toByteArray(0)
        val first = fixedHeader.first()
        val controlPacketType = first.toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun unsubscribe() {
        val expected = UNSUBSCRIBE
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun unsuback() {
        val expected = UNSUBACK
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun pingreq() {
        val expected = PINGREQ
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun pingresp() {
        val expected = PINGRESP
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun disconnect() {
        val expected = DISCONNECT
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun auth() {
        val expected = AUTH
        val fixedHeader = fromType(expected).toByteArray(0)
        val controlPacketType = fixedHeader.first().toControlPacketType()
        assertEquals(controlPacketType, expected)
    }

    @Test
    fun remainingLengthHandlesNegative1() {
        val expectedValue = -1
        try {
            fromType(CONNECT).toByteArray(expectedValue)
            fail("should of thrown an exception")
        } catch (e: MalformedInvalidVariableByteInteger) {
        }
    }

    @Test
    fun remainingLengthHandles0() {
        val expectedValue = 0
        val remainingLengthBytes = fromType(CONNECT).toByteArray(expectedValue)
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(1, remainingLengthBytes.size, "Incorrect byte array size")
        val actual = remainingLengthBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun remainingLengthHandles1() {
        val expectedValue = 1
        val remainingLengthBytes = fromType(CONNECT).toByteArray(expectedValue)
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(1, remainingLengthBytes.size, "Incorrect byte array size")
        val actual = remainingLengthBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun remainingLengthHandles127() {
        val expectedValue = 127
        val remainingLengthBytes = fromType(CONNECT).toByteArray(expectedValue)
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(1, remainingLengthBytes.size, "Incorrect byte array size")
        val actual = remainingLengthBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun remainingLengthHandles128() {
        val expectedValue = 128
        val remainingLengthBytes = fromType(CONNECT).toByteArray(expectedValue)
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(2, remainingLengthBytes.size, "Incorrect byte array size")
        val actual = remainingLengthBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun remainingLengthHandles16383() {
        val expectedValue = 16383
        val remainingLengthBytes = fromType(CONNECT).toByteArray(expectedValue)
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(2, remainingLengthBytes.size, "Incorrect byte array size")
        val actual = remainingLengthBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun remainingLengthHandles16384() {
        val expectedValue = 16384
        val remainingLengthBytes = fromType(CONNECT).toByteArray(expectedValue)
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(3, remainingLengthBytes.size, "Incorrect byte array size")
        val actual = remainingLengthBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun remainingLengthHandles65535() {
        val expectedValue = 65535
        val remainingLengthBytes = fromType(CONNECT).toByteArray(expectedValue)
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(3, remainingLengthBytes.size, "Incorrect byte array size")
        val actual = remainingLengthBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun remainingLengthHandlesMaxMinus1() {
        val expectedValue = VARIABLE_BYTE_INT_MAX - 1
        val remainingLengthBytes = fromType(CONNECT).toByteArray(expectedValue)
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(4, remainingLengthBytes.size, "Incorrect byte array size")
        val actual = remainingLengthBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }

    @Test
    fun remainingLengthHandlesMax() {
        val expectedValue = VARIABLE_BYTE_INT_MAX
        val remainingLengthBytes = fromType(CONNECT).toByteArray(expectedValue)
                .drop(1) // drop the first byte since we dont care about the control packet type
                .toByteArray()
        assertEquals(4, remainingLengthBytes.size, "Incorrect byte array size")
        val actual = remainingLengthBytes.decodeVariableByteInteger()
        assertEquals(expectedValue, actual, "Failed to read remaining bytes properly")
    }


    @Test
    fun remainingLengthHandlesMaxPlus1() {
        val expectedValue = VARIABLE_BYTE_INT_MAX + 1
        try {
            fromType(CONNECT).toByteArray(expectedValue)
                    .drop(1) // drop the first byte since we dont care about the control packet type
                    .toByteArray()
            fail("should of thrown an error")
        } catch (e: MalformedInvalidVariableByteInteger) {
        }
    }
}