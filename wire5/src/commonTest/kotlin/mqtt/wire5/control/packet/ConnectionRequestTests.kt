@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.MqttWarning
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.fixed.get
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.AT_MOST_ONCE
import mqtt.wire5.control.packet.ConnectionRequest.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.*
import kotlin.test.*

class ConnectionRequestTests {

    @Test
    fun serializeDefaults() {
        val connectionRequest = ConnectionRequest<Unit>()
        val buffer = allocateNewBuffer(15u, limits)
        connectionRequest.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00010000, buffer.readByte(), "invalid byte 1 on the CONNECT fixed header")
        assertEquals(
            13,
            buffer.readVariableByteInteger().toInt(),
            "invalid remaining length on the CONNECT fixed header"
        )
        assertEquals(0, buffer.readByte(), "invalid byte 1 on the CONNECT variable header (Length MSB (0))")
        assertEquals(4, buffer.readByte(), "invalid byte 2 on the CONNECT variable header (Length LSB (4))")
        assertEquals('M', buffer.readByte().toChar(), "invalid byte 3 on the CONNECT variable header")
        assertEquals('Q', buffer.readByte().toChar(), "invalid byte 4 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 5 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 6 on the CONNECT variable header")
        assertEquals(5, buffer.readByte(), "invalid byte 7 on the CONNECT variable header")
        val connectFlagsPacked = buffer.readByte()
        assertFalse(
            connectFlagsPacked.toUByte().get(7),
            "invalid byte 8 bit 7 on the CONNECT variable header for username flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(6),
            "invalid byte 8 bit 6 on the CONNECT variable header for password flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(5),
            "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(4),
            "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag"
        )
        assertTrue(
            connectFlagsPacked.toUByte().get(3),
            "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag"
        )
        assertEquals(
            AT_LEAST_ONCE,
            QualityOfService.fromBooleans(connectFlagsPacked.toUByte().get(4), connectFlagsPacked.toUByte().get(3)),
            "invalid byte 8 bit 4-3 on the CONNECT variable header for willQosBit flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(2),
            "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(1),
            "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(0),
            "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag"
        )
        assertEquals(UShort.MAX_VALUE, buffer.readUnsignedShort(), "invalid keep alive")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "client id")
    }

    @Test
    fun serializeAtMostOnce() {
        val connectionRequest = ConnectionRequest<Unit>(VariableHeader(willQos = AT_MOST_ONCE))
        val buffer = allocateNewBuffer(15u, limits)
        assertEquals(11u, connectionRequest.variableHeader.size(buffer), "variable header size")
        assertEquals(2u, connectionRequest.payload.size(buffer), "payload size")
        connectionRequest.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00010000, buffer.readByte(), "invalid byte 1 on the CONNECT fixed header")
        assertEquals(
            13,
            buffer.readVariableByteInteger().toInt(),
            "invalid remaining length on the CONNECT fixed header"
        )
        assertEquals(0, buffer.readByte(), "invalid byte 1 on the CONNECT variable header (Length MSB (0))")
        assertEquals(4, buffer.readByte(), "invalid byte 2 on the CONNECT variable header (Length LSB (4))")
        assertEquals('M', buffer.readByte().toChar(), "invalid byte 3 on the CONNECT variable header")
        assertEquals('Q', buffer.readByte().toChar(), "invalid byte 4 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 5 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 6 on the CONNECT variable header")
        assertEquals(5, buffer.readByte(), "invalid byte 7 on the CONNECT variable header")
        val connectFlagsPacked = buffer.readByte()
        assertFalse(
            connectFlagsPacked.toUByte().get(7),
            "invalid byte 8 bit 7 on the CONNECT variable header for username flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(6),
            "invalid byte 8 bit 6 on the CONNECT variable header for password flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(5),
            "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(4),
            "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(3),
            "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag"
        )
        assertEquals(
            AT_MOST_ONCE,
            QualityOfService.fromBooleans(connectFlagsPacked.toUByte().get(4), connectFlagsPacked.toUByte().get(3)),
            "invalid byte 8 bit 4-3 on the CONNECT variable header for willQosBit flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(2),
            "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(1),
            "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(0),
            "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag"
        )
        assertEquals(UShort.MAX_VALUE, buffer.readUnsignedShort(), "invalid keep alive")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "client id")
    }

    @Test
    fun serializeAtMostOnceHasUsername() {
        val connectionRequest = ConnectionRequest<Unit>(
            VariableHeader(willQos = AT_MOST_ONCE, hasUserName = true),
            ConnectionRequest.Payload(userName = "yolo")
        )
        val buffer = allocateNewBuffer(21u, limits)
        connectionRequest.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00010000, buffer.readByte(), "invalid byte 1 on the CONNECT fixed header")
        assertEquals(
            19,
            buffer.readVariableByteInteger().toInt(),
            "invalid remaining length on the CONNECT fixed header"
        )
        assertEquals(0, buffer.readByte(), "invalid byte 1 on the CONNECT variable header (Length MSB (0))")
        assertEquals(4, buffer.readByte(), "invalid byte 2 on the CONNECT variable header (Length LSB (4))")
        assertEquals('M', buffer.readByte().toChar(), "invalid byte 3 on the CONNECT variable header")
        assertEquals('Q', buffer.readByte().toChar(), "invalid byte 4 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 5 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 6 on the CONNECT variable header")
        assertEquals(5, buffer.readByte(), "invalid byte 7 on the CONNECT variable header")
        val connectFlagsPacked = buffer.readByte()
        assertTrue(
            connectFlagsPacked.toUByte().get(7),
            "invalid byte 8 bit 7 on the CONNECT variable header for username flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(6),
            "invalid byte 8 bit 6 on the CONNECT variable header for password flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(5),
            "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(4),
            "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(3),
            "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag"
        )
        assertEquals(
            AT_MOST_ONCE,
            QualityOfService.fromBooleans(connectFlagsPacked.toUByte().get(4), connectFlagsPacked.toUByte().get(3)),
            "invalid byte 8 bit 4-3 on the CONNECT variable header for willQosBit flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(2),
            "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(1),
            "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(0),
            "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag"
        )
        assertEquals(UShort.MAX_VALUE, buffer.readUnsignedShort(), "invalid keep alive")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "client id")
        assertEquals("yolo", buffer.readMqttUtf8StringNotValidated().toString(), "username")
    }

    @Test
    fun serializeAtMostOnceHasPassword() {
        val connectionRequest = ConnectionRequest<Unit>(
            VariableHeader(willQos = AT_MOST_ONCE, hasPassword = true),
            ConnectionRequest.Payload(password = "yolo")
        )
        val buffer = allocateNewBuffer(21u, limits)
        connectionRequest.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00010000, buffer.readByte(), "invalid byte 1 on the CONNECT fixed header")
        assertEquals(
            19,
            buffer.readVariableByteInteger().toInt(),
            "invalid remaining length on the CONNECT fixed header"
        )
        assertEquals(0, buffer.readByte(), "invalid byte 1 on the CONNECT variable header (Length MSB (0))")
        assertEquals(4, buffer.readByte(), "invalid byte 2 on the CONNECT variable header (Length LSB (4))")
        assertEquals('M', buffer.readByte().toChar(), "invalid byte 3 on the CONNECT variable header")
        assertEquals('Q', buffer.readByte().toChar(), "invalid byte 4 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 5 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 6 on the CONNECT variable header")
        assertEquals(5, buffer.readByte(), "invalid byte 7 on the CONNECT variable header")
        val connectFlagsPacked = buffer.readByte()
        assertFalse(
            connectFlagsPacked.toUByte().get(7),
            "invalid byte 8 bit 7 on the CONNECT variable header for username flag"
        )
        assertTrue(
            connectFlagsPacked.toUByte().get(6),
            "invalid byte 8 bit 6 on the CONNECT variable header for password flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(5),
            "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(4),
            "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(3),
            "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag"
        )
        assertEquals(
            AT_MOST_ONCE,
            QualityOfService.fromBooleans(connectFlagsPacked.toUByte().get(4), connectFlagsPacked.toUByte().get(3)),
            "invalid byte 8 bit 4-3 on the CONNECT variable header for willQosBit flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(2),
            "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(1),
            "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(0),
            "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag"
        )
        assertEquals(UShort.MAX_VALUE, buffer.readUnsignedShort(), "invalid keep alive")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "client id")
        assertEquals("yolo", buffer.readMqttUtf8StringNotValidated().toString(), "password")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasWillRetainCheckWarning() {
        assertNotNull(
            VariableHeader(willQos = AT_MOST_ONCE, willRetain = true).validateOrGetWarning(),
            "should of provided an warning"
        )
    }


    @Test
    fun serializeAtMostOnceHasWillRetain() {
        val connectionRequest = ConnectionRequest<Unit>(VariableHeader(willQos = AT_MOST_ONCE, willRetain = true))
        val buffer = allocateNewBuffer(15u, limits)
        connectionRequest.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00010000, buffer.readByte(), "invalid byte 1 on the CONNECT fixed header")
        assertEquals(
            13,
            buffer.readVariableByteInteger().toInt(),
            "invalid remaining length on the CONNECT fixed header"
        )
        assertEquals(0, buffer.readByte(), "invalid byte 1 on the CONNECT variable header (Length MSB (0))")
        assertEquals(4, buffer.readByte(), "invalid byte 2 on the CONNECT variable header (Length LSB (4))")
        assertEquals('M', buffer.readByte().toChar(), "invalid byte 3 on the CONNECT variable header")
        assertEquals('Q', buffer.readByte().toChar(), "invalid byte 4 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 5 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 6 on the CONNECT variable header")
        assertEquals(5, buffer.readByte(), "invalid byte 7 on the CONNECT variable header")
        val connectFlagsPacked = buffer.readByte()
        assertFalse(
            connectFlagsPacked.toUByte().get(7),
            "invalid byte 8 bit 7 on the CONNECT variable header for username flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(6),
            "invalid byte 8 bit 6 on the CONNECT variable header for password flag"
        )
        assertTrue(
            connectFlagsPacked.toUByte().get(5),
            "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(4),
            "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(3),
            "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag"
        )
        assertEquals(
            AT_MOST_ONCE,
            QualityOfService.fromBooleans(connectFlagsPacked.toUByte().get(4), connectFlagsPacked.toUByte().get(3)),
            "invalid byte 8 bit 4-3 on the CONNECT variable header for willQosBit flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(2),
            "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(1),
            "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(0),
            "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag"
        )
        assertEquals(UShort.MAX_VALUE, buffer.readUnsignedShort(), "invalid keep alive")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "client id")
    }

    @Test
    fun serializeExactlyOnce() {
        val connectionRequest = ConnectionRequest<Unit>(VariableHeader(willQos = QualityOfService.EXACTLY_ONCE))
        val buffer = allocateNewBuffer(15u, limits)
        connectionRequest.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00010000, buffer.readByte(), "invalid byte 1 on the CONNECT fixed header")
        assertEquals(
            13,
            buffer.readVariableByteInteger().toInt(),
            "invalid remaining length on the CONNECT fixed header"
        )
        assertEquals(0, buffer.readByte(), "invalid byte 1 on the CONNECT variable header (Length MSB (0))")
        assertEquals(4, buffer.readByte(), "invalid byte 2 on the CONNECT variable header (Length LSB (4))")
        assertEquals('M', buffer.readByte().toChar(), "invalid byte 3 on the CONNECT variable header")
        assertEquals('Q', buffer.readByte().toChar(), "invalid byte 4 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 5 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 6 on the CONNECT variable header")
        assertEquals(5, buffer.readByte(), "invalid byte 7 on the CONNECT variable header")
        val connectFlagsPacked = buffer.readByte()
        assertFalse(
            connectFlagsPacked.toUByte().get(7),
            "invalid byte 8 bit 7 on the CONNECT variable header for username flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(6),
            "invalid byte 8 bit 6 on the CONNECT variable header for password flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(5),
            "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag"
        )
        assertTrue(
            connectFlagsPacked.toUByte().get(4),
            "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(3),
            "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag"
        )
        assertEquals(
            QualityOfService.EXACTLY_ONCE,
            QualityOfService.fromBooleans(connectFlagsPacked.toUByte().get(4), connectFlagsPacked.toUByte().get(3)),
            "invalid byte 8 bit 4-3 on the CONNECT variable header for willQosBit flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(2),
            "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(1),
            "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(0),
            "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag"
        )
        assertEquals(UShort.MAX_VALUE, buffer.readUnsignedShort(), "invalid keep alive")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "client id")
    }

    @Test
    fun serializeAtMostOnceWillFlagTrue() {
        val connectionRequest = ConnectionRequest<Unit>(VariableHeader(willQos = AT_MOST_ONCE, willFlag = true))
        val buffer = allocateNewBuffer(15u, limits)
        connectionRequest.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00010000, buffer.readByte(), "invalid byte 1 on the CONNECT fixed header")
        assertEquals(
            13,
            buffer.readVariableByteInteger().toInt(),
            "invalid remaining length on the CONNECT fixed header"
        )
        assertEquals(0, buffer.readByte(), "invalid byte 1 on the CONNECT variable header (Length MSB (0))")
        assertEquals(4, buffer.readByte(), "invalid byte 2 on the CONNECT variable header (Length LSB (4))")
        assertEquals('M', buffer.readByte().toChar(), "invalid byte 3 on the CONNECT variable header")
        assertEquals('Q', buffer.readByte().toChar(), "invalid byte 4 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 5 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 6 on the CONNECT variable header")
        assertEquals(5, buffer.readByte(), "invalid byte 7 on the CONNECT variable header")
        val connectFlagsPacked = buffer.readByte()
        assertFalse(
            connectFlagsPacked.toUByte().get(7),
            "invalid byte 8 bit 7 on the CONNECT variable header for username flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(6),
            "invalid byte 8 bit 6 on the CONNECT variable header for password flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(5),
            "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(4),
            "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(3),
            "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag"
        )
        assertEquals(
            AT_MOST_ONCE,
            QualityOfService.fromBooleans(connectFlagsPacked.toUByte().get(4), connectFlagsPacked.toUByte().get(3)),
            "invalid byte 8 bit 4-3 on the CONNECT variable header for willQosBit flag"
        )
        assertTrue(
            connectFlagsPacked.toUByte().get(2),
            "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(1),
            "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(0),
            "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag"
        )
        assertEquals(UShort.MAX_VALUE, buffer.readUnsignedShort(), "invalid keep alive")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "client id")
    }

    @Test
    fun variableHeaderConnectFlagsByte8HasCleanStart() {
        val connectionRequest = ConnectionRequest<Unit>(VariableHeader(willQos = AT_MOST_ONCE, cleanStart = true))
        val buffer = allocateNewBuffer(15u, limits)
        connectionRequest.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00010000, buffer.readByte(), "invalid byte 1 on the CONNECT fixed header")
        assertEquals(
            13,
            buffer.readVariableByteInteger().toInt(),
            "invalid remaining length on the CONNECT fixed header"
        )
        assertEquals(0, buffer.readByte(), "invalid byte 1 on the CONNECT variable header (Length MSB (0))")
        assertEquals(4, buffer.readByte(), "invalid byte 2 on the CONNECT variable header (Length LSB (4))")
        assertEquals('M', buffer.readByte().toChar(), "invalid byte 3 on the CONNECT variable header")
        assertEquals('Q', buffer.readByte().toChar(), "invalid byte 4 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 5 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 6 on the CONNECT variable header")
        assertEquals(5, buffer.readByte(), "invalid byte 7 on the CONNECT variable header")
        val connectFlagsPacked = buffer.readByte()
        assertFalse(
            connectFlagsPacked.toUByte().get(7),
            "invalid byte 8 bit 7 on the CONNECT variable header for username flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(6),
            "invalid byte 8 bit 6 on the CONNECT variable header for password flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(5),
            "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(4),
            "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(3),
            "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag"
        )
        assertEquals(
            AT_MOST_ONCE,
            QualityOfService.fromBooleans(connectFlagsPacked.toUByte().get(4), connectFlagsPacked.toUByte().get(3)),
            "invalid byte 8 bit 4-3 on the CONNECT variable header for willQosBit flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(2),
            "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag"
        )
        assertTrue(
            connectFlagsPacked.toUByte().get(1),
            "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(0),
            "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag"
        )
        assertEquals(UShort.MAX_VALUE, buffer.readUnsignedShort(), "invalid keep alive")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "client id")
    }


    @Test
    fun variableHeaderKeepAliveMax() {
        val connectionRequest = ConnectionRequest<Unit>(VariableHeader(keepAliveSeconds = 4))
        val buffer = allocateNewBuffer(15u, limits)
        connectionRequest.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00010000, buffer.readByte(), "invalid byte 1 on the CONNECT fixed header")
        assertEquals(
            13,
            buffer.readVariableByteInteger().toInt(),
            "invalid remaining length on the CONNECT fixed header"
        )
        assertEquals(0, buffer.readByte(), "invalid byte 1 on the CONNECT variable header (Length MSB (0))")
        assertEquals(4, buffer.readByte(), "invalid byte 2 on the CONNECT variable header (Length LSB (4))")
        assertEquals('M', buffer.readByte().toChar(), "invalid byte 3 on the CONNECT variable header")
        assertEquals('Q', buffer.readByte().toChar(), "invalid byte 4 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 5 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 6 on the CONNECT variable header")
        assertEquals(5, buffer.readByte(), "invalid byte 7 on the CONNECT variable header")
        val connectFlagsPacked = buffer.readByte()
        assertFalse(
            connectFlagsPacked.toUByte().get(7),
            "invalid byte 8 bit 7 on the CONNECT variable header for username flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(6),
            "invalid byte 8 bit 6 on the CONNECT variable header for password flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(5),
            "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(4),
            "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag"
        )
        assertTrue(
            connectFlagsPacked.toUByte().get(3),
            "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag"
        )
        assertEquals(
            AT_LEAST_ONCE,
            QualityOfService.fromBooleans(connectFlagsPacked.toUByte().get(4), connectFlagsPacked.toUByte().get(3)),
            "invalid byte 8 bit 4-3 on the CONNECT variable header for willQosBit flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(2),
            "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(1),
            "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(0),
            "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag"
        )
        assertEquals(4u, buffer.readUnsignedShort(), "invalid keep alive")
        assertEquals(0u, buffer.readVariableByteInteger(), "property length")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "client id")
    }

    @Test
    fun sessionExpiry() {
        val props = VariableHeader.Properties(sessionExpiryIntervalSeconds = 1L)
        val connectionRequest = ConnectionRequest<Unit>(VariableHeader(properties = props))
        val buffer = allocateNewBuffer(20u, limits)
        connectionRequest.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00010000, buffer.readByte(), "invalid byte 1 on the CONNECT fixed header")
        assertEquals(
            18,
            buffer.readVariableByteInteger().toInt(),
            "invalid remaining length on the CONNECT fixed header"
        )
        assertEquals(0, buffer.readByte(), "invalid byte 1 on the CONNECT variable header (Length MSB (0))")
        assertEquals(4, buffer.readByte(), "invalid byte 2 on the CONNECT variable header (Length LSB (4))")
        assertEquals('M', buffer.readByte().toChar(), "invalid byte 3 on the CONNECT variable header")
        assertEquals('Q', buffer.readByte().toChar(), "invalid byte 4 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 5 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 6 on the CONNECT variable header")
        assertEquals(5, buffer.readByte(), "invalid byte 7 on the CONNECT variable header")
        val connectFlagsPacked = buffer.readByte()
        assertFalse(
            connectFlagsPacked.toUByte().get(7),
            "invalid byte 8 bit 7 on the CONNECT variable header for username flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(6),
            "invalid byte 8 bit 6 on the CONNECT variable header for password flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(5),
            "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(4),
            "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag"
        )
        assertTrue(
            connectFlagsPacked.toUByte().get(3),
            "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag"
        )
        assertEquals(
            AT_LEAST_ONCE,
            QualityOfService.fromBooleans(connectFlagsPacked.toUByte().get(4), connectFlagsPacked.toUByte().get(3)),
            "invalid byte 8 bit 4-3 on the CONNECT variable header for willQosBit flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(2),
            "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(1),
            "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(0),
            "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag"
        )
        assertEquals(UShort.MAX_VALUE, buffer.readUnsignedShort(), "invalid keep alive")
        assertEquals(5u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x11, buffer.readByte(), "property identifier")
        assertEquals(1u, buffer.readUnsignedInt(), "session expiry interval seconds")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "client id")
    }

    @Test
    fun variableHeaderPropertySessionExpiryIntervalSeconds() {
        val props = VariableHeader.Properties.from(setOf(SessionExpiryInterval(5)))
        assertEquals(props.sessionExpiryIntervalSeconds, 5L)
    }

    @Test
    fun variableHeaderPropertySessionExpiryIntervalSecondsProtocolExceptionMultipleTimes() {
        try {
            VariableHeader.Properties.from(listOf(SessionExpiryInterval(5), SessionExpiryInterval(5)))
            fail("Should of hit a protocol exception for adding two session expiry intervals")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyReceiveMaximum() {
        val props = VariableHeader.Properties.from(setOf(ReceiveMaximum(5)))
        assertEquals(props.receiveMaximum, 5)
        val buffer = allocateNewBuffer(18u, limits)
        val request = ConnectionRequest<Unit>(VariableHeader(properties = props))
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as ConnectionRequest<*>
        assertEquals(requestRead.variableHeader.properties.receiveMaximum, 5)
    }

    @Test
    fun variableHeaderPropertyReceiveMaximumMultipleTimes() {
        try {
            VariableHeader.Properties.from(listOf(ReceiveMaximum(5), ReceiveMaximum(5)))
            fail("Should of hit a protocol exception for adding two receive maximums")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyReceiveMaximumSetTo0() {
        try {
            VariableHeader.Properties.from(setOf(ReceiveMaximum(0)))
            fail("Should of hit a protocol exception for setting 0 as the receive maximum")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun maximumPacketSizeCannotBeSetToZero() {
        try {
            VariableHeader.Properties.from(setOf(MaximumPacketSize(0L)))
            fail("should of thrown an exception")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyMaximumPacketSize() {
        val props = VariableHeader.Properties.from(setOf(MaximumPacketSize(5)))
        assertEquals(props.maximumPacketSize, 5)
        val buffer = allocateNewBuffer(20u, limits)
        val request = ConnectionRequest<Unit>(VariableHeader(properties = props))
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as ConnectionRequest<*>
        assertEquals(requestRead.variableHeader.properties.maximumPacketSize, 5)
    }

    @Test
    fun variableHeaderPropertyMaximumPacketSizeMultipleTimes() {
        try {
            VariableHeader.Properties.from(listOf(MaximumPacketSize(5), MaximumPacketSize(5)))
            fail("Should of hit a protocol exception for adding two maximum packet sizes")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyMaximumPacketSizeZeroValue() {
        try {
            VariableHeader.Properties.from(setOf(MaximumPacketSize(0)))
            fail("Should of hit a protocol exception for adding two maximum packet sizes")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyTopicAliasMaximum() {
        val props = VariableHeader.Properties.from(setOf(TopicAliasMaximum(5)))
        assertEquals(props.topicAliasMaximum, 5)
        val buffer = allocateNewBuffer(18u, limits)
        val request = ConnectionRequest<Unit>(VariableHeader(properties = props))
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as ConnectionRequest<*>
        assertEquals(requestRead.variableHeader.properties.topicAliasMaximum, 5)
    }

    @Test
    fun variableHeaderPropertyTopicAliasMaximumMultipleTimes() {
        try {
            VariableHeader.Properties.from(listOf(TopicAliasMaximum(5), TopicAliasMaximum(5)))
            fail("Should of hit a protocol exception for adding two topic alias maximums")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyRequestResponseInformation() {
        val props = VariableHeader.Properties.from(setOf(RequestResponseInformation(true)))
        assertEquals(props.requestResponseInformation, true)
        val buffer = allocateNewBuffer(18u, limits)
        val request = ConnectionRequest<Unit>(VariableHeader(properties = props))
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as ConnectionRequest<*>
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

        val buffer = allocateNewBuffer(17u, limits)
        val request = ConnectionRequest<Unit>(VariableHeader(properties = props))
        request.serialize(buffer)
        buffer.resetForRead()
        assertEquals(0b00010000, buffer.readByte(), "invalid byte 1 on the CONNECT fixed header")
        assertEquals(
            15,
            buffer.readVariableByteInteger().toInt(),
            "invalid remaining length on the CONNECT fixed header"
        )
        assertEquals(0, buffer.readByte(), "invalid byte 1 on the CONNECT variable header (Length MSB (0))")
        assertEquals(4, buffer.readByte(), "invalid byte 2 on the CONNECT variable header (Length LSB (4))")
        assertEquals('M', buffer.readByte().toChar(), "invalid byte 3 on the CONNECT variable header")
        assertEquals('Q', buffer.readByte().toChar(), "invalid byte 4 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 5 on the CONNECT variable header")
        assertEquals('T', buffer.readByte().toChar(), "invalid byte 6 on the CONNECT variable header")
        assertEquals(5, buffer.readByte(), "invalid byte 7 on the CONNECT variable header")
        val connectFlagsPacked = buffer.readByte()
        assertFalse(
            connectFlagsPacked.toUByte().get(7),
            "invalid byte 8 bit 7 on the CONNECT variable header for username flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(6),
            "invalid byte 8 bit 6 on the CONNECT variable header for password flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(5),
            "invalid byte 8 bit 5 on the CONNECT variable header for willRetain flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(4),
            "invalid byte 8 bit 4 on the CONNECT variable header for willQosBit4 flag"
        )
        assertTrue(
            connectFlagsPacked.toUByte().get(3),
            "invalid byte 8 bit 3 on the CONNECT variable header for willQosBit3 flag"
        )
        assertEquals(
            AT_LEAST_ONCE,
            QualityOfService.fromBooleans(connectFlagsPacked.toUByte().get(4), connectFlagsPacked.toUByte().get(3)),
            "invalid byte 8 bit 4-3 on the CONNECT variable header for willQosBit flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(2),
            "invalid byte 8 bit 2 on the CONNECT variable header for willFlag flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(1),
            "invalid byte 8 bit 1 on the CONNECT variable header for cleanStart flag"
        )
        assertFalse(
            connectFlagsPacked.toUByte().get(0),
            "invalid byte 8 bit 0 on the CONNECT variable header for reserved flag"
        )
        assertEquals(UShort.MAX_VALUE, buffer.readUnsignedShort(), "invalid keep alive")
        assertEquals(2u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x17, buffer.readByte(), "missing property identifier request problem info")
        assertEquals(1, buffer.readByte(), "incorrect request problem info flag")
        assertEquals("", buffer.readMqttUtf8StringNotValidated().toString(), "client id")
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as ConnectionRequest<*>
        assertEquals(true, requestRead.variableHeader.properties.requestProblemInformation)
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
        val props = VariableHeader.Properties.from(setOf(UserProperty("key", "value")))
        val userPropertyResult = props.userProperty!!
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)
        val buffer = allocateNewBuffer(28u, limits)
        val request = ConnectionRequest<Unit>(VariableHeader(properties = props))
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as ConnectionRequest<*>
        val (key, value) = requestRead.variableHeader.properties.userProperty!!.first()
        assertEquals("key", key.toString())
        assertEquals("value", value.toString())
    }

    @Test
    fun variableHeaderPropertyUserPropertyMultipleTimes() {
        val userProperty = UserProperty("key", "value")
        val props = VariableHeader.Properties.from(listOf(userProperty, userProperty))
        val userPropertyResult = props.userProperty!!
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 2)
    }

    @Test
    fun variableHeaderPropertyAuth() {
        val payload = ByteArrayWrapper(byteArrayOf(1, 2, 3))
        val method = AuthenticationMethod("yolo")
        val data = AuthenticationData(payload)
        val props = VariableHeader.Properties.from(setOf(method, data))
        val auth = props.authentication!!

        assertEquals(auth.method, "yolo")
        assertEquals(auth.data, ByteArrayWrapper(byteArrayOf(1, 2, 3)))

        val buffer = allocateNewBuffer(28u, limits)
        val request = ConnectionRequest<Unit>(VariableHeader(properties = props))
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as ConnectionRequest<*>
        assertEquals("yolo", requestRead.variableHeader.properties.authentication!!.method.toString())
        assertEquals(
            ByteArrayWrapper(byteArrayOf(1, 2, 3)),
            requestRead.variableHeader.properties.authentication!!.data
        )
    }

    @Test
    fun variableHeaderPropertyAuthMethodsMultipleTimes() {
        val method = AuthenticationMethod("yolo")
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
        val method = ServerReference("yolo")
        try {
            VariableHeader.Properties.from(listOf(method, method))
            fail("Should of hit a protocol exception for adding an invalid connect header")
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun packetQos0() {
        val buffer = allocateNewBuffer(15u, limits)
        val request = ConnectionRequest<Unit>(
            VariableHeader("".toCharSequenceBuffer(), willQos = AT_MOST_ONCE),
            ConnectionRequest.Payload("")
        )
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as ConnectionRequest<*>
        assertEquals(request.variableHeader, requestRead.variableHeader)
    }

    @Test
    fun payloadFormatIndicatorInVariableHeader() {
        try {
            VariableHeader.Properties.from(setOf(PayloadFormatIndicator(true)))
            fail("Should of thrown a malformed packet exception. Payload Format Indicator is not a valid connect variable header property, it is a will property")
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun usernameFlagMatchesPayloadFailureCaseNoFlagWithUsername() {
        try {
            val connectionRequest = ConnectionRequest<Unit>(
                payload = ConnectionRequest.Payload(userName = "yolo")
            )
            val warning = connectionRequest.validateOrGetWarning()
            if (warning != null) throw warning
            fail()
        } catch (e: MqttWarning) {
        }
    }

    @Test
    fun usernameFlagMatchesPayloadFailureCaseWithFlagNoUsername() {
        try {
            val connectionRequest = ConnectionRequest<Unit>(VariableHeader(hasUserName = true))
            val warning = connectionRequest.validateOrGetWarning()
            if (warning != null) throw warning
            fail()
        } catch (e: MqttWarning) {
        }
    }

    @Test
    fun passwordFlagMatchesPayloadFailureCaseNoFlagWithUsername() {
        try {
            val connectionRequest = ConnectionRequest<Unit>(
                payload = ConnectionRequest.Payload(password = "yolo")
            )
            val warning = connectionRequest.validateOrGetWarning()
            if (warning != null) throw warning
            fail()
        } catch (e: MqttWarning) {
        }
    }

    @Test
    fun passwordFlagMatchesPayloadFailureCaseWithFlagNoUsername() {
        try {
            val connectionRequest = ConnectionRequest<Unit>(VariableHeader(hasPassword = true))
            val warning = connectionRequest.validateOrGetWarning()
            if (warning != null) throw warning
            fail()
        } catch (e: MqttWarning) {
        }
    }

}
