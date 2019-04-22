@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import mqtt.wire.ProtocolError
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VariableByteInteger
import mqtt.wire5.control.packet.PublishComplete.VariableHeader
import mqtt.wire5.control.packet.format.ReasonCode.PACKET_IDENTIFIER_NOT_FOUND
import mqtt.wire5.control.packet.format.ReasonCode.RECEIVE_MAXIMUM_EXCEEDED
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class PublishCompleteTests {
    private val packetIdentifier = 2.toUShort()

    @Test
    fun packetIdentifier() {
        val puback = PublishComplete(VariableHeader(packetIdentifier))
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as PublishComplete
        assertEquals(pubackResult.variable.packetIdentifier, packetIdentifier)
    }

    @Test
    fun packetIdentifierSendDefaults() {
        val puback = PublishComplete(VariableHeader(packetIdentifier))
        val data = puback.serialize(true)
        val pubackResult = ControlPacketV5.from(data) as PublishComplete
        assertEquals(pubackResult.variable.packetIdentifier, packetIdentifier)
    }

    @Test
    fun noMatchingSubscribers() {
        val puback = PublishComplete(VariableHeader(packetIdentifier, PACKET_IDENTIFIER_NOT_FOUND))
        val data = puback.serialize()
        val pubackResult = ControlPacketV5.from(data) as PublishComplete
        assertEquals(pubackResult.variable.reasonCode, PACKET_IDENTIFIER_NOT_FOUND)
    }

    @Test
    fun invalidReasonCodeThrowsProtocolError() {
        try {
            PublishComplete(VariableHeader(packetIdentifier, RECEIVE_MAXIMUM_EXCEEDED))
            fail()
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun reasonString() {
        val actual = PublishComplete(VariableHeader(packetIdentifier, properties = VariableHeader.Properties(reasonString = MqttUtf8String("yolo"))))
        val bytes = actual.serialize()
        val expected = ControlPacketV5.from(bytes) as PublishComplete
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
            VariableHeader.Properties.from(props.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }


    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = VariableHeader.Properties.from(setOf(UserProperty(MqttUtf8String("key"), MqttUtf8String("value"))))
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key.getValueOrThrow(), "key")
            assertEquals(value.getValueOrThrow(), "value")
        }
        assertEquals(userPropertyResult.size, 1)

        val request = PublishComplete(VariableHeader(packetIdentifier, properties = props)).serialize()
        val requestRead = ControlPacketV5.from(request.copy()) as PublishComplete
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.getValueOrThrow(), "key")
        assertEquals(value.getValueOrThrow(), "value")
    }

}