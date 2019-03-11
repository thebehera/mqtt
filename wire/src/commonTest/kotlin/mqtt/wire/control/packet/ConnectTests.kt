@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException
import mqtt.wire.MqttWarning
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.ConnectionRequest.VariableHeader
import mqtt.wire.control.packet.format.variable.property.*
import mqtt.wire.data.*
import mqtt.wire.data.QualityOfService.AT_MOST_ONCE
import kotlin.test.*

class ConnectTests {

    @Test
    fun fixedHeaderByte1() {
        val connectionRequest = ConnectionRequest()
        val bytes = connectionRequest.serialize().readBytes()
        val byte1 = bytes.first()
        assertEquals(byte1, 0b00010000, "invalid byte 1 on the CONNECT fixed header")
    }

    @Test
    fun fixedHeaderRemainingLength() {
        val connectionRequest = ConnectionRequest()
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte

        val remainingLength = byteReader.decodeVariableByteInteger()
        assertEquals(remainingLength.toInt(), 13, "invalid remaining length on the CONNECT fixed header")
    }

    @Test
    fun variableHeaderProtocolNameByte1() {
        val connectionRequest = ConnectionRequest()
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        val byte1ProtocolName = byteReader.readByte()
        assertEquals(byte1ProtocolName, 0, "invalid byte 1 on the CONNECT variable header")
    }

    @Test
    fun variableHeaderProtocolNameByte2() {
        val connectionRequest = ConnectionRequest()
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        val byte = byteReader.readByte()
        assertEquals(byte, 0b100, "invalid byte 2 on the CONNECT variable header")
        assertEquals(byte, 4, "invalid byte 2 on the CONNECT variable header")
    }

    @Test
    fun variableHeaderProtocolNameByte3() {
        val connectionRequest = ConnectionRequest()
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        val byte = byteReader.readByte()
        assertEquals(byte, 0b01001101, "invalid byte 3 on the CONNECT variable header")
        assertEquals(byte.toChar(), 'M', "invalid byte 3 on the CONNECT variable header")
    }

    @Test
    fun variableHeaderProtocolNameByte4() {
        val connectionRequest = ConnectionRequest()
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        val byte = byteReader.readByte()
        assertEquals(byte, 0b01010001, "invalid byte 4 on the CONNECT variable header")
        assertEquals(byte.toChar(), 'Q', "invalid byte 4 on the CONNECT variable header")
    }

    @Test
    fun variableHeaderProtocolNameByte5() {
        val connectionRequest = ConnectionRequest()
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        val byte = byteReader.readByte()
        assertEquals(byte, 0b01010100, "invalid byte 5 on the CONNECT variable header")
        assertEquals(byte.toChar(), 'T', "invalid byte 5 on the CONNECT variable header")
    }

    @Test
    fun variableHeaderProtocolNameByte6() {
        val connectionRequest = ConnectionRequest()
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        val byte = byteReader.readByte()
        assertEquals(byte, 0b01010100, "invalid byte 6 on the CONNECT variable header")
        assertEquals(byte.toChar(), 'T', "invalid byte 6 on the CONNECT variable header")
    }

    @Test
    fun variableHeaderProtocolVersionByte7() {
        val connectionRequest = ConnectionRequest()
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        val byte = byteReader.readByte()
        assertEquals(byte, 0b00000101, "invalid byte 7 on the CONNECT variable header")
        assertEquals(byte, 5, "invalid byte 7 on the CONNECT variable header")
    }

    @Test
    fun variableHeaderConnectFlagsByte8AllFalse() {
        val connectionRequest = ConnectionRequest(VariableHeader(willQos = AT_MOST_ONCE))
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        val byte = byteReader.readUByte()
        val connectFlagsPackedInByte = byte.toInt()
        val usernameFlag = connectFlagsPackedInByte.shr(7) == 1
        assertFalse(usernameFlag, "invalid byte 8 bit 7 on the CONNECT variable header for username flag")
        val passwordFlag = connectFlagsPackedInByte.shl(1).shr(7) == 1
        assertFalse(passwordFlag, "invalid byte 8 bit 6 on the CONNECT variable header for password flag")
        val willRetain = connectFlagsPackedInByte.shl(2).shr(7) == 1
        assertFalse(willRetain, "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag")
        val willQosBit4 = connectFlagsPackedInByte.shl(3).shr(7) == 1
        assertFalse(willQosBit4, "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag")
        val willQosBit3 = connectFlagsPackedInByte.shl(4).shr(7) == 1
        assertFalse(willQosBit3, "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag")
        val willQos = QualityOfService.fromBooleans(willQosBit4, willQosBit3)
        assertEquals(willQos, AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertFalse(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertFalse(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasUsername() {
        val connectionRequest = ConnectionRequest(VariableHeader(willQos = AT_MOST_ONCE, hasUserName = true),
                ConnectionRequest.Payload(userName = MqttUtf8String("yolo")))
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        val byte = byteReader.readUByte()
        val connectFlagsPackedInByte = byte.toInt()
        val usernameFlag = connectFlagsPackedInByte.shr(7) == 1
        assertTrue(usernameFlag, "invalid byte 8 bit 7 on the CONNECT variable header for username flag")
        val passwordFlag = connectFlagsPackedInByte.shl(1).shr(7) == 1
        assertFalse(passwordFlag, "invalid byte 8 bit 6 on the CONNECT variable header for password flag")
        val willRetain = connectFlagsPackedInByte.shl(2).shr(7) == 1
        assertFalse(willRetain, "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag")
        val willQosBit4 = connectFlagsPackedInByte.shl(3).shr(7) == 1
        assertFalse(willQosBit4, "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag")
        val willQosBit3 = connectFlagsPackedInByte.shl(4).shr(7) == 1
        assertFalse(willQosBit3, "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag")
        val willQos = QualityOfService.fromBooleans(willQosBit4, willQosBit3)
        assertEquals(willQos, AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertFalse(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertFalse(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasPassword() {
        val connectionRequest = ConnectionRequest(VariableHeader(willQos = AT_MOST_ONCE, hasPassword = true),
                ConnectionRequest.Payload(password = MqttUtf8String("yolo")))
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        val byte = byteReader.readUByte()
        val connectFlagsPackedInByte = byte.toInt()
        val usernameFlag = connectFlagsPackedInByte.shr(7) == 1
        assertFalse(usernameFlag, "invalid byte 8 bit 7 on the CONNECT variable header for username flag")
        val passwordFlag = connectFlagsPackedInByte.shl(1).shr(7) == 1
        assertTrue(passwordFlag, "invalid byte 8 bit 6 on the CONNECT variable header for password flag")
        val willRetain = connectFlagsPackedInByte.shl(2).shr(7) == 1
        assertFalse(willRetain, "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag")
        val willQosBit4 = connectFlagsPackedInByte.shl(3).shr(7) == 1
        assertFalse(willQosBit4, "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag")
        val willQosBit3 = connectFlagsPackedInByte.shl(4).shr(7) == 1
        assertFalse(willQosBit3, "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag")
        val willQos = QualityOfService.fromBooleans(willQosBit4, willQosBit3)
        assertEquals(willQos, AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertFalse(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertFalse(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasWillRetainCheckWarning() {
        assertNotNull(VariableHeader(willQos = AT_MOST_ONCE, willRetain = true).validateOrGetWarning(),
                "should of provided an warning")
    }


    @Test
    fun variableHeaderConnectFlagsByte8HasWillRetain() {
        val vh = VariableHeader(willQos = AT_MOST_ONCE, willRetain = true)
        val connectionRequest = ConnectionRequest(vh)
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        val byte = byteReader.readUByte()
        val connectFlagsPackedInByte = byte.toInt()
        val usernameFlag = connectFlagsPackedInByte.shr(7) == 1
        assertFalse(usernameFlag, "invalid byte 8 bit 7 on the CONNECT variable header for username flag")
        val passwordFlag = connectFlagsPackedInByte.shl(1).shr(7) == 1
        assertFalse(passwordFlag, "invalid byte 8 bit 6 on the CONNECT variable header for password flag")
        val willRetain = connectFlagsPackedInByte.shl(2).shr(7) == 1
        assertTrue(willRetain, "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag")
        val willQosBit4 = connectFlagsPackedInByte.shl(3).shr(7) == 1
        assertFalse(willQosBit4, "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag")
        val willQosBit3 = connectFlagsPackedInByte.shl(4).shr(7) == 1
        assertFalse(willQosBit3, "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag")
        val willQos = QualityOfService.fromBooleans(willQosBit4, willQosBit3)
        assertEquals(willQos, AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertFalse(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertFalse(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }


    @Test
    fun variableHeaderConnectFlagsByte8HasQos1() {
        val connectionRequest = ConnectionRequest()
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        val byte = byteReader.readUByte()
        val connectFlagsPackedInByte = byte.toInt()
        val usernameFlag = connectFlagsPackedInByte.shr(7) == 1
        assertFalse(usernameFlag, "invalid byte 8 bit 7 on the CONNECT variable header for username flag")
        val passwordFlag = connectFlagsPackedInByte.shl(1).shr(7) == 1
        assertFalse(passwordFlag, "invalid byte 8 bit 6 on the CONNECT variable header for password flag")
        val willRetain = connectFlagsPackedInByte.shl(2).shr(7) == 1
        assertFalse(willRetain, "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag")
        val willQosBit4 = connectFlagsPackedInByte.shl(3).shr(7) == 1
        assertFalse(willQosBit4, "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag")
        val willQosBit3 = connectFlagsPackedInByte.shl(4).shr(7) == 1
        assertTrue(willQosBit3, "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag")
        val willQos = QualityOfService.fromBooleans(willQosBit4, willQosBit3)
        assertEquals(willQos, QualityOfService.AT_LEAST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertFalse(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertFalse(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasQos2() {
        val connectionRequest = ConnectionRequest(VariableHeader(willQos = QualityOfService.EXACTLY_ONCE))
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        val byte = byteReader.readUByte()
        val connectFlagsPackedInByte = byte.toInt()
        val usernameFlag = connectFlagsPackedInByte.shr(7) == 1
        assertFalse(usernameFlag, "invalid byte 8 bit 7 on the CONNECT variable header for username flag")
        val passwordFlag = connectFlagsPackedInByte.shl(1).shr(7) == 1
        assertFalse(passwordFlag, "invalid byte 8 bit 6 on the CONNECT variable header for password flag")
        val willRetain = connectFlagsPackedInByte.shl(2).shr(7) == 1
        assertFalse(willRetain, "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag")
        val willQosBit4 = connectFlagsPackedInByte.shl(3).shr(7) == 1
        assertTrue(willQosBit4, "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag")
        val willQosBit3 = connectFlagsPackedInByte.shl(4).shr(7) == 1
        assertFalse(willQosBit3, "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag")
        val willQos = QualityOfService.fromBooleans(willQosBit4, willQosBit3)
        assertEquals(willQos, QualityOfService.EXACTLY_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertFalse(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertFalse(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasWillFlagThrowWarning() {
        val connectionRequest = ConnectionRequest(VariableHeader(willQos = AT_MOST_ONCE, willFlag = true))
        try {
            connectionRequest.serialize()
            fail("should of thrown an exception since the default is to throw on warnings")
        } catch (e: MqttWarning) {
        }
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasWillFlag() {
        val connectionRequest = ConnectionRequest(VariableHeader(willQos = AT_MOST_ONCE, willFlag = true))
        val byteReader = connectionRequest.serialize(throwOnWarning = false).copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        val byte = byteReader.readUByte()
        val connectFlagsPackedInByte = byte.toInt()
        val usernameFlag = connectFlagsPackedInByte.shr(7) == 1
        assertFalse(usernameFlag, "invalid byte 8 bit 7 on the CONNECT variable header for username flag")
        val passwordFlag = connectFlagsPackedInByte.shl(1).shr(7) == 1
        assertFalse(passwordFlag, "invalid byte 8 bit 6 on the CONNECT variable header for password flag")
        val willRetain = connectFlagsPackedInByte.shl(2).shr(7) == 1
        assertFalse(willRetain, "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag")
        val willQosBit4 = connectFlagsPackedInByte.shl(3).shr(7) == 1
        assertFalse(willQosBit4, "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag")
        val willQosBit3 = connectFlagsPackedInByte.shl(4).shr(7) == 1
        assertFalse(willQosBit3, "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag")
        val willQos = QualityOfService.fromBooleans(willQosBit4, willQosBit3)
        assertEquals(willQos, AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertTrue(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertFalse(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasCleanStart() {
        val connectionRequest = ConnectionRequest(VariableHeader(willQos = AT_MOST_ONCE, cleanStart = true))
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        val byte = byteReader.readUByte()
        val connectFlagsPackedInByte = byte.toInt()
        val usernameFlag = connectFlagsPackedInByte.shr(7) == 1
        assertFalse(usernameFlag, "invalid byte 8 bit 7 on the CONNECT variable header for username flag")
        val passwordFlag = connectFlagsPackedInByte.shl(1).shr(7) == 1
        assertFalse(passwordFlag, "invalid byte 8 bit 6 on the CONNECT variable header for password flag")
        val willRetain = connectFlagsPackedInByte.shl(2).shr(7) == 1
        assertFalse(willRetain, "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag")
        val willQosBit4 = connectFlagsPackedInByte.shl(3).shr(7) == 1
        assertFalse(willQosBit4, "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag")
        val willQosBit3 = connectFlagsPackedInByte.shl(4).shr(7) == 1
        assertFalse(willQosBit3, "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag")
        val willQos = QualityOfService.fromBooleans(willQosBit4, willQosBit3)
        assertEquals(willQos, AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertFalse(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertTrue(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }


    @Test
    fun variableHeaderKeepAliveDefault() {
        val connectionRequest = ConnectionRequest()
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        byteReader.readByte() // connect flags
        val keepAliveSeconds = byteReader.readUShort() // read byte 9 and 10 since UShort is 2 Bytes
        assertEquals(keepAliveSeconds, connectionRequest.variableHeader.keepAliveSeconds)
        assertEquals(UShort.MAX_VALUE, connectionRequest.variableHeader.keepAliveSeconds)
    }

    @Test
    fun variableHeaderKeepAlive0() {
        val connectionRequest = ConnectionRequest(VariableHeader(keepAliveSeconds = 0.toUShort()))
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        byteReader.readByte() // connect flags
        val keepAliveSeconds = byteReader.readUShort() // read byte 9 and 10 since UShort is 2 Bytes
        assertEquals(keepAliveSeconds, connectionRequest.variableHeader.keepAliveSeconds)
        assertEquals(0.toUShort(), connectionRequest.variableHeader.keepAliveSeconds)
    }


    @Test
    fun variableHeaderKeepAliveMax() {
        val connectionRequest = ConnectionRequest(VariableHeader(keepAliveSeconds = UShort.MAX_VALUE))
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        byteReader.readByte() // connect flags
        val keepAliveSeconds = byteReader.readUShort() // read byte 9 and 10 since UShort is 2 Bytes
        assertEquals(keepAliveSeconds, connectionRequest.variableHeader.keepAliveSeconds)
        assertEquals(UShort.MAX_VALUE, connectionRequest.variableHeader.keepAliveSeconds)
    }

    @Test
    fun propertyLengthEmpty() {
        val connectionRequest = ConnectionRequest()
        val bytes = connectionRequest.serialize().readBytes()
        val byteReader = ByteReadPacket(bytes)
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        byteReader.readByte() // connect flags
        byteReader.readUShort() // read byte 9 and 10 since UShort is 2 Bytes
        val propertyIndexStart = bytes.size
        val propertyLength = byteReader.decodeVariableByteInteger()
        val propertyBytesLength = propertyIndexStart - bytes.size
        val propertiesBytes = connectionRequest.variableHeader.properties.packet.readBytes()
        val propertySize = propertiesBytes.size - propertyBytesLength
        assertEquals(propertyLength, propertySize.toUInt())
        assertEquals(propertyLength, 0.toUInt())
    }

    @Test
    fun propertyLengthSessionExpiry() {
        val props = VariableHeader.Properties(sessionExpiryIntervalSeconds = 1.toUInt())
        val connectionRequest = ConnectionRequest(VariableHeader(properties = props))
        val byteReader = connectionRequest.serialize().copy()
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        byteReader.readByte() // Length MSB (0)
        byteReader.readByte() // Length LSB (4)
        byteReader.readByte() // 'M' or 0b01001101
        byteReader.readByte() // 'Q' or 0b01010001
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 'T' or 0b01010100
        byteReader.readByte() // 5 or 0b00000101
        byteReader.readByte() // connect flags
        byteReader.readUShort() // read byte 9 and 10 since UShort is 2 Bytes
        val properties = byteReader.readProperties()
        assertNotNull(properties!!.first())
//        assertEquals(properties.first().property, Property.SessionExpiryInterval)
    }

    @Test
    fun variableHeaderPropertySessionExpiryIntervalSeconds() {
        val props = VariableHeader.Properties.from(setOf(SessionExpiryInterval(5.toUInt())))
        assertEquals(props.sessionExpiryIntervalSeconds, 5.toUInt())
    }

    @Test
    fun variableHeaderPropertySessionExpiryIntervalSecondsProtocolExceptionMultipleTimes() {
        try {
            VariableHeader.Properties.from(listOf(SessionExpiryInterval(5.toUInt()), SessionExpiryInterval(5.toUInt())))
            fail("Should of hit a protocol exception for adding two session expiry intervals")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyReceiveMaximum() {
        val props = VariableHeader.Properties.from(setOf(ReceiveMaximum(5.toUShort())))
        assertEquals(props.receiveMaximum, 5.toUShort())

        val request = ConnectionRequest(VariableHeader(properties = props)).serialize()
        val requestRead = ControlPacket.from(request.copy()) as ConnectionRequest
        assertEquals(requestRead.variableHeader.properties.receiveMaximum, 5.toUShort())
    }

    @Test
    fun variableHeaderPropertyReceiveMaximumMultipleTimes() {
        try {
            VariableHeader.Properties.from(listOf(ReceiveMaximum(5.toUShort()), ReceiveMaximum(5.toUShort())))
            fail("Should of hit a protocol exception for adding two receive maximums")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyReceiveMaximumSetTo0() {
        try {
            VariableHeader.Properties.from(setOf(ReceiveMaximum(0.toUShort())))
            fail("Should of hit a protocol exception for setting 0 as the receive maximum")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun maximumPacketSizeCannotBeSetToZero() {
        try {
            VariableHeader.Properties.from(setOf(MaximumPacketSize(0.toUInt())))
            fail("should of thrown an exception")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyMaximumPacketSize() {
        val props = VariableHeader.Properties.from(setOf(MaximumPacketSize(5.toUInt())))
        assertEquals(props.maximumPacketSize, 5.toUInt())

        val request = ConnectionRequest(VariableHeader(properties = props)).serialize()
        val requestRead = ControlPacket.from(request.copy()) as ConnectionRequest
        assertEquals(requestRead.variableHeader.properties.maximumPacketSize, 5.toUInt())
    }

    @Test
    fun variableHeaderPropertyMaximumPacketSizeMultipleTimes() {
        try {
            VariableHeader.Properties.from(listOf(MaximumPacketSize(5.toUInt()), MaximumPacketSize(5.toUInt())))
            fail("Should of hit a protocol exception for adding two maximum packet sizes")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyMaximumPacketSizeZeroValue() {
        try {
            VariableHeader.Properties.from(setOf(MaximumPacketSize(0.toUInt())))
            fail("Should of hit a protocol exception for adding two maximum packet sizes")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyTopicAliasMaximum() {
        val props = VariableHeader.Properties.from(setOf(TopicAliasMaximum(5.toUShort())))
        assertEquals(props.topicAliasMaximum, 5.toUShort())

        val request = ConnectionRequest(VariableHeader(properties = props)).serialize()
        val requestRead = ControlPacket.from(request.copy()) as ConnectionRequest
        assertEquals(requestRead.variableHeader.properties.topicAliasMaximum, 5.toUShort())
    }

    @Test
    fun variableHeaderPropertyTopicAliasMaximumMultipleTimes() {
        try {
            VariableHeader.Properties.from(listOf(TopicAliasMaximum(5.toUShort()), TopicAliasMaximum(5.toUShort())))
            fail("Should of hit a protocol exception for adding two topic alias maximums")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyRequestResponseInformation() {
        val props = VariableHeader.Properties.from(setOf(RequestResponseInformation(true)))
        assertEquals(props.requestResponseInformation, true)

        val request = ConnectionRequest(VariableHeader(properties = props)).serialize()
        val requestRead = ControlPacket.from(request.copy()) as ConnectionRequest
        assertEquals(requestRead.variableHeader.properties.requestResponseInformation, true)
    }

    @Test
    fun variableHeaderPropertyRequestResponseInformationMultipleTimes() {
        try {
            VariableHeader.Properties.from(listOf(RequestResponseInformation(true), RequestResponseInformation(true)))
            fail("Should of hit a protocol exception for adding two Request Response Information")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyRequestProblemInformation() {
        val props = VariableHeader.Properties.from(setOf(RequestProblemInformation(true)))
        assertEquals(props.requestProblemInformation, true)

        val request = ConnectionRequest(VariableHeader(properties = props)).serialize()
        val requestRead = ControlPacket.from(request.copy()) as ConnectionRequest
        assertEquals(requestRead.variableHeader.properties.requestProblemInformation, true)
    }

    @Test
    fun variableHeaderPropertyRequestProblemInformationMultipleTimes() {
        try {
            VariableHeader.Properties.from(listOf(RequestProblemInformation(true), RequestProblemInformation(true)))
            fail("Should of hit a protocol exception for adding two Request Problem Information")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = VariableHeader.Properties.from(setOf(UserProperty(MqttUtf8String("key"), MqttUtf8String("value"))))
        val userPropertyResult = props.userProperty!!
        for ((key, value) in userPropertyResult) {
            assertEquals(key.getValueOrThrow(), "key")
            assertEquals(value.getValueOrThrow(), "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request = ConnectionRequest(VariableHeader(properties = props)).serialize()
        val requestRead = ControlPacket.from(request.copy()) as ConnectionRequest
        val (key, value) = requestRead.variableHeader.properties.userProperty!!.first()
        assertEquals(key.getValueOrThrow(), "key")
        assertEquals(value.getValueOrThrow(), "value")
    }

    @Test
    fun variableHeaderPropertyUserPropertyMultipleTimes() {
        val userProperty = UserProperty(MqttUtf8String("key"), MqttUtf8String("value"))
        val props = VariableHeader.Properties.from(listOf(userProperty, userProperty))
        val userPropertyResult = props.userProperty!!
        for ((key, value) in userPropertyResult) {
            assertEquals(key.getValueOrThrow(), "key")
            assertEquals(value.getValueOrThrow(), "value")
        }
        assertEquals(userPropertyResult.size, 2)
    }

    @Test
    fun variableHeaderPropertyAuth() {
        val payload = ByteArrayWrapper(byteArrayOf(1, 2, 3))
        val method = AuthenticationMethod(MqttUtf8String("yolo"))
        val data = AuthenticationData(payload)
        val props = VariableHeader.Properties.from(setOf(method, data))
        val auth = props.authentication!!

        assertEquals(auth.method.getValueOrThrow(), "yolo")
        assertEquals(auth.data, ByteArrayWrapper(byteArrayOf(1, 2, 3)))

        val request = ConnectionRequest(VariableHeader(properties = props)).serialize()
        val requestRead = ControlPacket.from(request.copy()) as ConnectionRequest
        assertEquals(requestRead.variableHeader.properties.authentication!!.method.getValueOrThrow(), "yolo")
        assertEquals(requestRead.variableHeader.properties.authentication!!.data, ByteArrayWrapper(byteArrayOf(1, 2, 3)))

    }

    @Test
    fun variableHeaderPropertyAuthMethodsMultipleTimes() {
        val method = AuthenticationMethod(MqttUtf8String("yolo"))
        try {
            VariableHeader.Properties.from(listOf(method, method))
            fail("Should of hit a protocol exception for adding two Auth Methods")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyAuthDataMultipleTimes() {
        val payload = ByteArrayWrapper(byteArrayOf(1, 2, 3))
        val data = AuthenticationData(payload)
        try {
            VariableHeader.Properties.from(listOf(data, data))
            fail("Should of hit a protocol exception for adding two Auth Data")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyInvalid() {
        val method = ServerReference(MqttUtf8String("yolo"))
        try {
            VariableHeader.Properties.from(listOf(method, method))
            fail("Should of hit a protocol exception for adding an invalid connect header")
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun packetDefault() {
        val request = ConnectionRequest()
        val byteArray = request.serialize().copy()
        val requestDeserialized = ControlPacket.from(byteArray)
        assertEquals(requestDeserialized, request)
    }

    @Test
    fun packetQos0() {
        val request = ConnectionRequest(VariableHeader(willQos = AT_MOST_ONCE))
        val byteArray = request.serialize().copy()
        val requestDeserialized = ControlPacket.from(byteArray)
        assertEquals(requestDeserialized, request)
    }

    @Test
    fun payloadFormatIndicatorInVariableHeader() {
        try {
            ConnectionRequest.VariableHeader.Properties.from(setOf(PayloadFormatIndicator(true)))
            fail("Should of thrown a malformed packet exception. Payload Format Indicator is not a valid connect variable header property, it is a will property")
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun willPropertiesDefaultBasic() {
        val actual = ConnectionRequest.Payload.WillProperties()
        val data = actual.packet().readBytes()
        assertEquals(ByteArrayWrapper(byteArrayOf(0)), ByteArrayWrapper(data))
        val expected = ConnectionRequest.Payload.WillProperties.from(ByteReadPacket(data))
        assertEquals(actual, expected)
    }

    @Test
    fun willPropertiesDefaultBasicSendDefaults() {
        val actual = ConnectionRequest.Payload.WillProperties()
        val data = actual.packet(true).readBytes()
        assertEquals(ByteArrayWrapper(byteArrayOf(7, 24, 0, 0, 0, 0, 1, 0)), ByteArrayWrapper(data))
        val expected = ConnectionRequest.Payload.WillProperties.from(ByteReadPacket(data))
        assertEquals(actual, expected)
    }

    @Test
    fun willPropertiesChangeDelayInterval() {
        val actual = ConnectionRequest.Payload.WillProperties(willDelayIntervalSeconds = 4.toUInt())
        val data = actual.packet().readBytes()
        assertEquals(ByteArrayWrapper(byteArrayOf(5, 24, 0, 0, 0, 4)), ByteArrayWrapper(data))
        val expected = ConnectionRequest.Payload.WillProperties.from(ByteReadPacket(data))
        assertEquals(actual, expected)
    }

    @Test
    fun willPropertiesChangeDelayIntervalSendDefaults() {
        val actual = ConnectionRequest.Payload.WillProperties(willDelayIntervalSeconds = 4.toUInt())
        val data = actual.packet(true).readBytes()
        assertEquals(ByteArrayWrapper(byteArrayOf(7, 24, 0, 0, 0, 4, 1, 0)), ByteArrayWrapper(data))
        val expected = ConnectionRequest.Payload.WillProperties.from(ByteReadPacket(data))
        assertEquals(actual, expected)
    }

    @Test
    fun willPropertiesDefault() {
        val willProps = ConnectionRequest.Payload.WillProperties()
        val header = VariableHeader(willFlag = true)
        val payload = ConnectionRequest.Payload(
                willProperties = willProps,
                willPayload = ByteArrayWrapper("Swag".toByteArray()),
                willTopic = MqttUtf8String("/yolo"))
        val connectionRequestPreSerialized = ConnectionRequest(header, payload)
        assertNull(connectionRequestPreSerialized.validateOrGetWarning())
        val serialized = connectionRequestPreSerialized.serialize().copy()
        val connectionRequest = ControlPacket.from(serialized) as ConnectionRequest
        assertEquals(connectionRequest.payload.willProperties?.willDelayIntervalSeconds, 0.toUInt())
        assertEquals(connectionRequest.payload.willPayload, ByteArrayWrapper("Swag".toByteArray()))
        assertEquals(connectionRequest.payload.willTopic, MqttUtf8String("/yolo"))
    }

    @Test
    fun willPropertiesWillDelayIntervalProtocolError() {
        val data = buildPacket {
            WillDelayInterval(1.toUInt()).write(this)
            WillDelayInterval(2.toUInt()).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        try {
            ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
            fail("should of hit an error")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun willPropertiesPayloadFormatIndicator() {
        val data = buildPacket {
            PayloadFormatIndicator(true).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        val willProps = ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
        val withoutDefaults = willProps.packet()
        val willPropsWithoutDefaults = ConnectionRequest.Payload.WillProperties.from(withoutDefaults.copy())
        assertTrue(willPropsWithoutDefaults.payloadFormatIndicator)
        val withDefaults = willProps.packet(true)
        val willPropsWithDefaults = ConnectionRequest.Payload.WillProperties.from(withDefaults.copy())
        assertTrue(willPropsWithDefaults.payloadFormatIndicator)
    }

    @Test
    fun willPropertiesMessageExpiryInterval() {
        val data = buildPacket {
            MessageExpiryInterval(4.toUInt()).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        val willProps = ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
        val withoutDefaults = willProps.packet()
        val willPropsWithoutDefaults = ConnectionRequest.Payload.WillProperties.from(withoutDefaults.copy())
        assertEquals(willPropsWithoutDefaults.messageExpiryIntervalSeconds, 4.toUInt())
        val withDefaults = willProps.packet(true)
        val willPropsWithDefaults = ConnectionRequest.Payload.WillProperties.from(withDefaults.copy())
        assertEquals(willPropsWithDefaults.messageExpiryIntervalSeconds, 4.toUInt())
    }

    @Test
    fun willPropertiesMessageExpiryIntervalProtocolError() {
        val data = buildPacket {
            MessageExpiryInterval(1.toUInt()).write(this)
            MessageExpiryInterval(2.toUInt()).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        try {
            ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
            fail("should of hit an error")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun willPropertiesContentType() {
        val data = buildPacket {
            ContentType(MqttUtf8String("f")).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        val willProps = ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
        val withoutDefaults = willProps.packet()
        val willPropsWithoutDefaults = ConnectionRequest.Payload.WillProperties.from(withoutDefaults.copy())
        assertEquals(willPropsWithoutDefaults.contentType!!.getValueOrThrow(), "f")
        val withDefaults = willProps.packet(true)
        val willPropsWithDefaults = ConnectionRequest.Payload.WillProperties.from(withDefaults.copy())
        assertEquals(willPropsWithDefaults.contentType!!.getValueOrThrow(), "f")
    }

    @Test
    fun willPropertiesContentTypeProtocolError() {
        val data = buildPacket {
            ContentType(MqttUtf8String("")).write(this)
            ContentType(MqttUtf8String("f")).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        try {
            ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
            fail("should of hit an error")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun willPropertiesResponseTopic() {
        val data = buildPacket {
            ResponseTopic(MqttUtf8String("f")).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        val willProps = ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
        val withoutDefaults = willProps.packet()
        val willPropsWithoutDefaults = ConnectionRequest.Payload.WillProperties.from(withoutDefaults.copy())
        assertEquals(willPropsWithoutDefaults.responseTopic!!.getValueOrThrow(), "f")
        val withDefaults = willProps.packet(true)
        val willPropsWithDefaults = ConnectionRequest.Payload.WillProperties.from(withDefaults.copy())
        assertEquals(willPropsWithDefaults.responseTopic!!.getValueOrThrow(), "f")
    }

    @Test
    fun willPropertiesResponseTopicProtocolError() {
        val data = buildPacket {
            ResponseTopic(MqttUtf8String("")).write(this)
            ResponseTopic(MqttUtf8String("f")).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        try {
            ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
            fail("should of hit an error")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun willPropertiesCoorelationData() {
        val data = buildPacket {
            CorrelationData(ByteArrayWrapper(byteArrayOf(0xF))).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        val willProps = ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
        val withoutDefaults = willProps.packet()
        val willPropsWithoutDefaults = ConnectionRequest.Payload.WillProperties.from(withoutDefaults.copy())
        assertEquals(willPropsWithoutDefaults.correlationData!!, ByteArrayWrapper(byteArrayOf(0xF)))
        val withDefaults = willProps.packet(true)
        val willPropsWithDefaults = ConnectionRequest.Payload.WillProperties.from(withDefaults.copy())
        assertEquals(willPropsWithDefaults.correlationData!!, ByteArrayWrapper(byteArrayOf(0xF)))
    }

    @Test
    fun willPropertiesCoorelationDataProtocolError() {
        val data = buildPacket {
            CorrelationData(ByteArrayWrapper("".toByteArray())).write(this)
            CorrelationData(ByteArrayWrapper("f".toByteArray())).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        try {
            ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
            fail("should of hit an error")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun willPropertiesUserDataOne() {
        val data = buildPacket {
            UserProperty(MqttUtf8String("k"), MqttUtf8String("v"))
                    .write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        val willProps = ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
        val withoutDefaults = willProps.packet()
        val willPropsWithoutDefaults = ConnectionRequest.Payload.WillProperties.from(withoutDefaults.copy())
        val (keyWithoutDefaults, valueWithoutDefaults) = willPropsWithoutDefaults.userProperty.first()
        assertEquals(keyWithoutDefaults.getValueOrThrow(), "k")
        assertEquals(valueWithoutDefaults.getValueOrThrow(), "v")

        val withDefaults = willProps.packet(true)
        val willPropsWithDefaults = ConnectionRequest.Payload.WillProperties.from(withDefaults.copy())
        val (keyWithDefaults, valueWithDefaults) = willPropsWithDefaults.userProperty.first()
        assertEquals(keyWithDefaults.getValueOrThrow(), "k")
        assertEquals(valueWithDefaults.getValueOrThrow(), "v")
    }

    @Test
    fun willPropertiesUserDataOneRead() {
        val data = buildPacket {
            UserProperty(MqttUtf8String("k"), MqttUtf8String("v"))
                    .write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        val willProps = ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)

        val (key, value) = willProps.userProperty.first()
        assertEquals(key.getValueOrThrow(), "k")
        assertEquals(value.getValueOrThrow(), "v")
    }

    @Test
    fun willPropertiesUserDataMultiple() {
        val data = buildPacket {
            UserProperty(MqttUtf8String("k0"), MqttUtf8String("v0")).write(this)
            UserProperty(MqttUtf8String("k1"), MqttUtf8String("v1")).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        val willProps = ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
        willProps.userProperty.forEachIndexed { index, (key, value) ->
            assertEquals(key.getValueOrThrow(), "k$index")
            assertEquals(value.getValueOrThrow(), "v$index")
        }
    }

    @Test
    fun willPropertiesInvalid() {
        val data = buildPacket {
            ReasonString(MqttUtf8String("k")).write(this)
        }.readBytes()
        val dataWithPropertyLength = buildPacket {
            writePacket(VariableByteInteger(data.size.toUInt()).encodedValue())
            writeFully(data)
        }.copy()
        try {
            ConnectionRequest.Payload.WillProperties.from(dataWithPropertyLength)
            fail("should of thrown an exception")
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun usernameFlagMatchesPayloadFailureCaseNoFlagWithUsername() {
        try {
            val connectionRequest = ConnectionRequest(
                    payload = ConnectionRequest.Payload(userName = MqttUtf8String("yolo")))
            val warning = connectionRequest.validateOrGetWarning()
            if (warning != null) throw warning
            fail()
        } catch (e: MqttWarning) {
        }
    }

    @Test
    fun usernameFlagMatchesPayloadFailureCaseWithFlagNoUsername() {
        try {
            val connectionRequest = ConnectionRequest(VariableHeader(hasUserName = true))
            val warning = connectionRequest.validateOrGetWarning()
            if (warning != null) throw warning
            fail()
        } catch (e: MqttWarning) {
        }
    }

    @Test
    fun passwordFlagMatchesPayloadFailureCaseNoFlagWithUsername() {
        try {
            val connectionRequest = ConnectionRequest(
                    payload = ConnectionRequest.Payload(password = MqttUtf8String("yolo")))
            val warning = connectionRequest.validateOrGetWarning()
            if (warning != null) throw warning
            fail()
        } catch (e: MqttWarning) {
        }
    }

    @Test
    fun passwordFlagMatchesPayloadFailureCaseWithFlagNoUsername() {
        try {
            val connectionRequest = ConnectionRequest(VariableHeader(hasPassword = true))
            val warning = connectionRequest.validateOrGetWarning()
            if (warning != null) throw warning
            fail()
        } catch (e: MqttWarning) {
        }
    }

}
