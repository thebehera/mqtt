@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.toByteArray
import kotlinx.io.core.writeFully
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode.BANNED
import mqtt.wire.control.packet.format.ReasonCode.SUCCESS
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VariableByteInteger
import mqtt.wire5.control.packet.AuthenticationExchange.VariableHeader
import mqtt.wire5.control.packet.AuthenticationExchange.VariableHeader.Properties
import mqtt.wire5.control.packet.format.variable.property.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class AuthenticationExchangeTests {
    @Test
    fun serializeDeserialize() {
        val props = Properties(MqttUtf8String("test"))
        val disconnect = AuthenticationExchange(VariableHeader(SUCCESS, props))
        val deserialized = ControlPacketV5.from(disconnect.serialize()) as AuthenticationExchange
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
        val props = Properties(MqttUtf8String("2"), reasonString = MqttUtf8String("yolo"))
        val header = VariableHeader(SUCCESS, properties = props)
        val actual = AuthenticationExchange(header)
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as AuthenticationExchange
        assertEquals(expected.variable.properties.reasonString, MqttUtf8String("yolo"))
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString(MqttUtf8String("yolo"))
        val obj2 = obj1.copy()
        val propsWithoutPropertyLength = buildPacket {
            obj1.write(this)
            obj2.write(this)
        }.readBytes()
        val props = buildPacket {
            writePacket(VariableByteInteger(propsWithoutPropertyLength.size.toUInt()).encodedValue())
            writeFully(propsWithoutPropertyLength)
        }.copy()
        try {
            Properties.from(props.readPropertiesLegacy())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = Properties.from(
                setOf(UserProperty(MqttUtf8String("key"), MqttUtf8String("value")),
                        UserProperty(MqttUtf8String("key"), MqttUtf8String("value"))))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key.getValueOrThrow(), "key")
            assertEquals(value.getValueOrThrow(), "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request = AuthenticationExchange(VariableHeader(SUCCESS, properties = props)).serialize()
        val requestRead = ControlPacketV5.from(request.copy()) as AuthenticationExchange
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.getValueOrThrow(), "key")
        assertEquals(value.getValueOrThrow(), "value")
    }

    @Test
    fun authMethodMultipleTimesThrowsProtocolError() {
        val obj1 = AuthenticationMethod(MqttUtf8String("yolo"))
        val obj2 = obj1.copy()
        val propsWithoutPropertyLength = buildPacket {
            obj1.write(this)
            obj2.write(this)
        }.readBytes()
        val props = buildPacket {
            writePacket(VariableByteInteger(propsWithoutPropertyLength.size.toUInt()).encodedValue())
            writeFully(propsWithoutPropertyLength)
        }.copy()
        try {
            Properties.from(props.readPropertiesLegacy())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun authDataMultipleTimesThrowsProtocolError() {
        val obj1 = AuthenticationData(ByteArrayWrapper("yas".toByteArray()))
        val obj2 = obj1.copy()
        val propsWithoutPropertyLength = buildPacket {
            obj1.write(this)
            obj2.write(this)
        }.readBytes()
        val props = buildPacket {
            writePacket(VariableByteInteger(propsWithoutPropertyLength.size.toUInt()).encodedValue())
            writeFully(propsWithoutPropertyLength)
        }.copy()
        try {
            Properties.from(props.readPropertiesLegacy())
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun invalidReasonCode() {
        try {
            VariableHeader(BANNED, Properties())
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
