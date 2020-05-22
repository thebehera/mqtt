@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.allocateNewBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode.BANNED
import mqtt.wire.control.packet.format.ReasonCode.SUCCESS
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire5.control.packet.AuthenticationExchange.VariableHeader
import mqtt.wire5.control.packet.AuthenticationExchange.VariableHeader.Properties
import mqtt.wire5.control.packet.format.variable.property.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

val limits = object : BufferMemoryLimit {
    override fun isTooLargeForMemory(size: UInt) = false
}

class AuthenticationExchangeTests {

    @Test
    fun serializationByteVerification() {
        val buffer = allocateNewBuffer(14u, limits)
        val props = Properties(Authentication("test", ByteArrayWrapper(byteArrayOf())))
        val disconnect = AuthenticationExchange(VariableHeader(SUCCESS, props))
        disconnect.serialize(buffer)
        buffer.resetForRead()
        // fixed header
        assertEquals(0b11110000.toUByte(), buffer.readUnsignedByte(), "byte1 fixed header")
        assertEquals(12u, buffer.readVariableByteInteger(), "byte2 fixed header remaining length")
        // variable header
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "byte0 variable header reason code")
        assertEquals(10u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x15.toUByte(), buffer.readUnsignedByte(), "identifier of the authentication method")
        assertEquals(4u, buffer.readUnsignedShort())
        assertEquals("test", buffer.readUtf8(4u).toString(), "authentication method value")
    }

    @Test
    fun serializeDeserialize() {
        val buffer = allocateNewBuffer(14u, limits)
        val props = Properties(Authentication("test".toCharSequenceBuffer(), ByteArrayWrapper(byteArrayOf())))
        val disconnect = AuthenticationExchange(VariableHeader(SUCCESS, props))
        disconnect.serialize(buffer)
        buffer.resetForRead()
        val deserialized = ControlPacketV5.from(buffer) as AuthenticationExchange
        assertEquals(deserialized.variable.reasonCode, SUCCESS)
        assertEquals(disconnect, deserialized)
    }

    @Test
    fun serializeDeserializeInvalid() {
        try {
            VariableHeader(BANNED, Properties(Authentication("test", ByteArrayWrapper(byteArrayOf()))))
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun reasonString() {
        val buffer = allocateNewBuffer(18u, limits)
        val props = Properties(
            Authentication("2".toCharSequenceBuffer(), ByteArrayWrapper(byteArrayOf())),
            reasonString = "yolo"
        )
        val header = VariableHeader(SUCCESS, properties = props)
        val expected = AuthenticationExchange(header)
        expected.serialize(buffer)
        buffer.resetForRead()
        val actual = ControlPacketV5.from(buffer) as AuthenticationExchange
        assertEquals("yolo", actual.variable.properties.reasonString.toString())
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString("yolo")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(20u, limits)
        val size = obj1.size(buffer) + obj2.size(buffer)
        buffer.writeVariableByteInteger(size)
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyByteValidation() {
        val props = Properties.from(
            setOf(
                AuthenticationMethod("2"),
                UserProperty("key", "value")
            )
        )
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val buffer = allocateNewBuffer(17u, limits)
        AuthenticationExchange(VariableHeader(SUCCESS, properties = props)).serialize(buffer)
        buffer.resetForRead()
        // fixed header
        assertEquals(0b11110000.toUByte(), buffer.readUnsignedByte(), "byte1 fixed header")
        assertEquals(15u, buffer.readVariableByteInteger(), "byte2 fixed header remaining length")
        // variable header
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "byte0 variable header reason code")
        assertEquals(13u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x26.toUByte(), buffer.readUnsignedByte(), "user property flag")
        assertEquals("key", buffer.readMqttUtf8StringNotValidated().toString(), "user property key")
        assertEquals("value", buffer.readMqttUtf8StringNotValidated().toString(), "user property value")
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = Properties.from(
            setOf(
                AuthenticationMethod("2"),
                UserProperty("key", "value")
            )
        )
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key")
            assertEquals(value, "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val buffer = allocateNewBuffer(21u, limits)
        AuthenticationExchange(VariableHeader(SUCCESS, properties = props)).serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as AuthenticationExchange
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.toString(), "key")
        assertEquals(value.toString(), "value")
    }

    @Test
    fun authMethodMultipleTimesThrowsProtocolError() {
        val obj1 = AuthenticationMethod("yolo")
        val buffer1 = allocateNewBuffer(20u, limits)
        val remainingLength = 2u * obj1.size(buffer1) + 1u
        buffer1.writeVariableByteInteger(remainingLength)
        obj1.write(buffer1)
        val obj2 = obj1.copy()
        obj2.write(buffer1)
        buffer1.resetForRead()
        assertFailsWith<ProtocolError>("should throw error because auth method is added twice") {
            Properties.from(buffer1.readProperties())
        }
    }

    @Test
    fun authDataMultipleTimesThrowsProtocolError() {
        val method = AuthenticationMethod("yolo")
        val obj1 = AuthenticationData(ByteArrayWrapper(byteArrayOf(1, 2, 3)))
        val buffer1 = allocateNewBuffer(20u, limits)
        buffer1.writeVariableByteInteger(19u)
        method.write(buffer1)
        obj1.write(buffer1)
        val obj2 = obj1.copy()
        obj2.write(buffer1)
        buffer1.resetForRead()
        assertFailsWith<ProtocolError>("should throw error because auth data is added twice") {
            Properties.from(buffer1.readProperties())
        }
    }

    @Test
    fun invalidReasonCode() {
        try {
            VariableHeader(BANNED, Properties(Authentication("test", ByteArrayWrapper(byteArrayOf()))))
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun invalidPropertyThrowsMalformedException() {
        try {
            Properties.from(setOf(WillDelayInterval(2)))
            fail()
        } catch (e: MalformedPacketException) {
        }
    }
}
