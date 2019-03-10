@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.readUByte
import kotlinx.io.core.readUShort
import mqtt.wire.control.packet.format.variable.property.SessionExpiryInterval
import mqtt.wire.control.packet.format.variable.property.readProperties
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.decodeVariableByteInteger
import kotlin.test.*

class ConnectTests {

    @Test
    fun fixedHeaderByte1() {
        val connectionRequest = ConnectionRequest()
        val bytes = connectionRequest.serialize
        val byte1 = bytes.first()
        assertEquals(byte1, 0b00010000, "invalid byte 1 on the CONNECT fixed header")
    }

    @Test
    fun fixedHeaderRemainingLength() {
        val connectionRequest = ConnectionRequest()
        val bytes = connectionRequest.serialize
        val byteReader = ByteReadPacket(bytes)
        byteReader.readByte() // skip the first byte

        val remainingLength = byteReader.decodeVariableByteInteger()
        assertEquals(remainingLength.toInt(), 13, "invalid remaining length on the CONNECT fixed header")
    }

    @Test
    fun variableHeaderProtocolNameByte1() {
        val connectionRequest = ConnectionRequest()
        val bytes = connectionRequest.serialize
        val byteReader = ByteReadPacket(bytes)
        byteReader.readByte() // skip the first byte
        byteReader.decodeVariableByteInteger() // skip the remaining length
        val byte1ProtocolName = byteReader.readByte()
        assertEquals(byte1ProtocolName, 0, "invalid byte 1 on the CONNECT variable header")
    }

    @Test
    fun variableHeaderProtocolNameByte2() {
        val connectionRequest = ConnectionRequest()
        val bytes = connectionRequest.serialize
        val byteReader = ByteReadPacket(bytes)
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
        val bytes = connectionRequest.serialize
        val byteReader = ByteReadPacket(bytes)
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
        val bytes = connectionRequest.serialize
        val byteReader = ByteReadPacket(bytes)
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
        val bytes = connectionRequest.serialize
        val byteReader = ByteReadPacket(bytes)
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
        val bytes = connectionRequest.serialize
        val byteReader = ByteReadPacket(bytes)
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
        val bytes = connectionRequest.serialize
        val byteReader = ByteReadPacket(bytes)
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
        val connectionRequest = ConnectionRequest(ConnectionRequest.VariableHeader(willQos = QualityOfService.AT_MOST_ONCE))
        val bytes = connectionRequest.serialize
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
        assertEquals(willQos, QualityOfService.AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertFalse(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertFalse(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasUsername() {
        val connectionRequest = ConnectionRequest(ConnectionRequest.VariableHeader(willQos = QualityOfService.AT_MOST_ONCE, hasUserName = true))
        val bytes = connectionRequest.serialize
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
        assertEquals(willQos, QualityOfService.AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertFalse(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertFalse(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasPassword() {
        val connectionRequest = ConnectionRequest(ConnectionRequest.VariableHeader(willQos = QualityOfService.AT_MOST_ONCE, hasPassword = true))
        val bytes = connectionRequest.serialize
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
        assertEquals(willQos, QualityOfService.AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertFalse(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertFalse(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasWillRetain() {
        val connectionRequest = ConnectionRequest(ConnectionRequest.VariableHeader(willQos = QualityOfService.AT_MOST_ONCE, willRetain = true))
        val bytes = connectionRequest.serialize
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
        assertEquals(willQos, QualityOfService.AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
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
        val bytes = connectionRequest.serialize
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
        val connectionRequest = ConnectionRequest(ConnectionRequest.VariableHeader(willQos = QualityOfService.EXACTLY_ONCE))
        val bytes = connectionRequest.serialize
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
    fun variableHeaderConnectFlagsByte8HasWillFlag() {
        val connectionRequest = ConnectionRequest(ConnectionRequest.VariableHeader(willQos = QualityOfService.AT_MOST_ONCE, willFlag = true))
        val bytes = connectionRequest.serialize
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
        assertEquals(willQos, QualityOfService.AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
        val willFlag = connectFlagsPackedInByte.shl(5).shr(7) == 1
        assertTrue(willFlag, "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag")
        val cleanStart = connectFlagsPackedInByte.shl(6).shr(7) == 1
        assertFalse(cleanStart, "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag")
        val reserved = connectFlagsPackedInByte.shl(7).shr(7) == 1
        assertFalse(reserved, "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasCleanStart() {
        val connectionRequest = ConnectionRequest(ConnectionRequest.VariableHeader(willQos = QualityOfService.AT_MOST_ONCE, cleanStart = true))
        val bytes = connectionRequest.serialize
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
        assertEquals(willQos, QualityOfService.AT_MOST_ONCE, "invalid byte 8 qos on the CONNECT variable header for willQos flag")
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
        val bytes = connectionRequest.serialize
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
        val keepAliveSeconds = byteReader.readUShort() // read byte 9 and 10 since UShort is 2 Bytes
        assertEquals(keepAliveSeconds, connectionRequest.variableHeader.keepAliveSeconds)
        assertEquals(UShort.MAX_VALUE, connectionRequest.variableHeader.keepAliveSeconds)
    }

    @Test
    fun variableHeaderKeepAlive0() {
        val connectionRequest = ConnectionRequest(ConnectionRequest.VariableHeader(keepAliveSeconds = 0.toUShort()))
        val bytes = connectionRequest.serialize
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
        val keepAliveSeconds = byteReader.readUShort() // read byte 9 and 10 since UShort is 2 Bytes
        assertEquals(keepAliveSeconds, connectionRequest.variableHeader.keepAliveSeconds)
        assertEquals(0.toUShort(), connectionRequest.variableHeader.keepAliveSeconds)
    }


    @Test
    fun variableHeaderKeepAliveMax() {
        val connectionRequest = ConnectionRequest(ConnectionRequest.VariableHeader(keepAliveSeconds = UShort.MAX_VALUE))
        val bytes = connectionRequest.serialize
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
        val keepAliveSeconds = byteReader.readUShort() // read byte 9 and 10 since UShort is 2 Bytes
        assertEquals(keepAliveSeconds, connectionRequest.variableHeader.keepAliveSeconds)
        assertEquals(UShort.MAX_VALUE, connectionRequest.variableHeader.keepAliveSeconds)
    }

    @Test
    fun propertyLengthEmpty() {
        val connectionRequest = ConnectionRequest()
        val bytes = connectionRequest.serialize
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
        val propertyIndexStart = byteReader.remaining
        val propertyLength = byteReader.decodeVariableByteInteger()
        val propertyBytesLength = propertyIndexStart - byteReader.remaining
        val propertiesBytes = connectionRequest.variableHeader.properties.packet
        val propertySize = propertiesBytes.size - propertyBytesLength
        assertEquals(propertyLength, propertySize.toUInt())
        assertEquals(propertyLength, 0.toUInt())
    }

    @Test
    fun propertyLengthSessionExpiry() {
        val props = ConnectionRequest.VariableHeader.Properties(sessionExpiryIntervalSeconds = 1.toUInt())
        val connectionRequest = ConnectionRequest(ConnectionRequest.VariableHeader(properties = props))
        val bytes = connectionRequest.serialize
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
        val properties = byteReader.readProperties()
        assertNotNull(properties.first())
//        assertEquals(properties.first().property, Property.SessionExpiryInterval)
    }

    @Test
    fun variableHeaderPropertySessionExpiryIntervalSeconds() {
        val props = ConnectionRequest.VariableHeader.Properties.from(setOf(SessionExpiryInterval(5.toUInt())))
        assertEquals(props.sessionExpiryIntervalSeconds, 5.toUInt())
    }

}
