@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import kotlinx.io.core.toByteArray
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.allocateNewBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode.BANNED
import mqtt.wire.control.packet.format.ReasonCode.SUCCESS
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.MqttUtf8String
import mqtt.wire5.control.packet.AuthenticationExchange.VariableHeader
import mqtt.wire5.control.packet.AuthenticationExchange.VariableHeader.Properties
import mqtt.wire5.control.packet.format.variable.property.*
import kotlin.test.*

val limits = object : BufferMemoryLimit {
    override fun isTooLargeForMemory(size: UInt) = false
}

class AuthenticationExchangeTests {

    @Test
    fun serializationByteVerification() {
        val buffer = allocateNewBuffer(11u, limits)
        val props = Properties(MqttUtf8String("test"))
        val disconnect = AuthenticationExchange(VariableHeader(SUCCESS, props))
        disconnect.serialize(buffer)
        buffer.resetForRead()
        // fixed header
        assertEquals(0b11110000.toUByte(), buffer.readUnsignedByte(), "byte1 fixed header")
        assertEquals(9u, buffer.readVariableByteInteger(), "byte2 fixed header remaining length")
        // variable header
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "byte0 variable header reason code")
        assertEquals(7u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x15.toUByte(), buffer.readUnsignedByte(), "identifier of the authentication method")
        assertEquals("test", buffer.readMqttUtf8StringNotValidated().toString(), "authentication method value")
    }

    @Test
    fun deserializationByteVerification() {
        val buffer = allocateNewBuffer(11u, limits)
        // Fill buffer
        buffer.write(0b11110000.toUByte()) // byte 1 fixed header
        buffer.writeVariableByteInteger(9u) // remaining length fixed header
        // Fill Buffer variable header
        buffer.write(SUCCESS.byte) // byte0 variable header reason code
        buffer.writeVariableByteInteger(5u) // property length
        buffer.write(0x15.toUByte()) // identifier of the authentication method
        buffer.writeUtf8String("test") // authentication method value
        buffer.resetForRead()
        val actual = ControlPacketV5.from(buffer) as AuthenticationExchange
        assertEquals(SUCCESS, actual.variable.reasonCode)
        assertEquals("test", actual.variable.properties.method.value.toString())
        assertNull(actual.variable.properties.data)
        assertNull(actual.variable.properties.reasonString)
        assertEquals(0, actual.variable.properties.userProperty.count())
    }

    @Test
    fun serializeDeserialize() {
        val buffer = allocateNewBuffer(11u, limits)
        val props = Properties(MqttUtf8String("test"))
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
            VariableHeader(BANNED, Properties(MqttUtf8String("test")))
            fail()
        } catch (e: MalformedPacketException) {
        }
    }

    @Test
    fun reasonString() {
        val buffer = allocateNewBuffer(15u, limits)
        val props = Properties(MqttUtf8String("2"), reasonString = MqttUtf8String("yolo"))
        val header = VariableHeader(SUCCESS, properties = props)
        val actual = AuthenticationExchange(header)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV5.from(buffer) as AuthenticationExchange
        assertEquals(expected.variable.properties.reasonString, MqttUtf8String("yolo"))
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString(MqttUtf8String("yolo"))
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
                AuthenticationMethod(MqttUtf8String("2")),
                UserProperty(MqttUtf8String("key"), MqttUtf8String("value"))
            )
        )
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key.getValueOrThrow(), "key")
            assertEquals(value.getValueOrThrow(), "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val buffer = allocateNewBuffer(21u, limits)
        AuthenticationExchange(VariableHeader(SUCCESS, properties = props)).serialize(buffer)
        buffer.resetForRead()
        // fixed header
        assertEquals(0b11110000.toUByte(), buffer.readUnsignedByte(), "byte1 fixed header")
        assertEquals(19u, buffer.readVariableByteInteger(), "byte2 fixed header remaining length")
        // variable header
        assertEquals(SUCCESS.byte, buffer.readUnsignedByte(), "byte0 variable header reason code")
        assertEquals(17u, buffer.readVariableByteInteger(), "property length")
        assertEquals(0x15.toUByte(), buffer.readUnsignedByte(), "identifier of the authentication method")
        assertEquals("2", buffer.readMqttUtf8StringNotValidated().toString(), "authentication method value")
        assertEquals(0x26.toUByte(), buffer.readUnsignedByte(), "user property flag")
        assertEquals("key", buffer.readMqttUtf8StringNotValidated().toString(), "user property key")
        assertEquals("value", buffer.readMqttUtf8StringNotValidated().toString(), "user property value")
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = Properties.from(
            setOf(
                AuthenticationMethod(MqttUtf8String("2")),
                UserProperty(MqttUtf8String("key"), MqttUtf8String("value"))
            )
        )
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key.getValueOrThrow(), "key")
            assertEquals(value.getValueOrThrow(), "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val buffer = allocateNewBuffer(21u, limits)
        AuthenticationExchange(VariableHeader(SUCCESS, properties = props)).serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as AuthenticationExchange
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.getValueOrThrow().toString(), "key")
        assertEquals(value.getValueOrThrow().toString(), "value")
    }

    @Test
    fun authMethodMultipleTimesThrowsProtocolError() {
        val obj1 = AuthenticationMethod(MqttUtf8String("yolo"))
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
        val method = AuthenticationMethod(MqttUtf8String("yolo"))
        val obj1 = AuthenticationData(ByteArrayWrapper("yas".toByteArray()))
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
            VariableHeader(BANNED, Properties(MqttUtf8String("test")))
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
