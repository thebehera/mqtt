@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode.PACKET_IDENTIFIER_NOT_FOUND
import mqtt.wire.control.packet.format.ReasonCode.RECEIVE_MAXIMUM_EXCEEDED
import mqtt.wire5.control.packet.PublishComplete.VariableHeader
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class PublishCompleteTests {
    private val packetIdentifier = 2

    @Test
    fun packetIdentifier() {
        val pubcomp = PublishComplete(VariableHeader(packetIdentifier))
        val buffer = allocateNewBuffer(4u, limits)
        pubcomp.serialize(buffer)
        buffer.resetForRead()
        val pubcompResult = ControlPacketV5.from(buffer) as PublishComplete
        assertEquals(pubcompResult.variable.packetIdentifier, packetIdentifier)
    }

    @Test
    fun packetIdentifierSendDefaults() {
        val pubcomp = PublishComplete(VariableHeader(packetIdentifier))
        val buffer = allocateNewBuffer(4u, limits)
        pubcomp.serialize(buffer)
        buffer.resetForRead()
        val pubcompResult = ControlPacketV5.from(buffer) as PublishComplete
        assertEquals(pubcompResult.variable.packetIdentifier, packetIdentifier)
    }

    @Test
    fun noMatchingSubscribers() {
        val pubcomp = PublishComplete(VariableHeader(packetIdentifier, PACKET_IDENTIFIER_NOT_FOUND))
        val buffer = allocateNewBuffer(6u, limits)
        pubcomp.serialize(buffer)
        buffer.resetForRead()
        val pubcompResult = ControlPacketV5.from(buffer) as PublishComplete
        assertEquals(pubcompResult.variable.reasonCode, PACKET_IDENTIFIER_NOT_FOUND)
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
        val expected = PublishComplete(
            VariableHeader(
                packetIdentifier,
                properties = VariableHeader.Properties(reasonString = "yolo")
            )
        )
        val buffer = allocateNewBuffer(13u, limits)
        expected.serialize(buffer)
        buffer.resetForRead()
        val actual = ControlPacketV5.from(buffer) as PublishComplete
        assertEquals(expected.variable.properties.reasonString.toString(), "yolo")
    }

    @Test
    fun reasonStringMultipleTimesThrowsProtocolError() {
        val obj1 = ReasonString("yolo")
        val obj2 = obj1.copy()
        val buffer = allocateNewBuffer(35u, limits)
        buffer.writeVariableByteInteger(obj1.size(buffer) + obj2.size(buffer))
        obj1.write(buffer)
        obj2.write(buffer)
        buffer.resetForRead()
        try {
            DisconnectNotification.VariableHeader.Properties.from(buffer.readProperties())
            fail()
        } catch (e: ProtocolError) {
        }
    }


    @Test
    fun variableHeaderPropertyUserProperty() {
        val props = VariableHeader.Properties.from(
            setOf(
                UserProperty(
                    "key".toCharSequenceBuffer(),
                    "value".toCharSequenceBuffer()
                )
            )
        )
        val userPropertyResult = props.userProperty
        for ((key, value) in userPropertyResult) {
            assertEquals(key, "key".toCharSequenceBuffer())
            assertEquals(value, "value".toCharSequenceBuffer())
        }
        assertEquals(userPropertyResult.size, 1)

        val buffer = allocateNewBuffer(19u, limits)
        val request = PublishComplete(VariableHeader(packetIdentifier, properties = props))
        request.serialize(buffer)
        buffer.resetForRead()
        val requestRead = ControlPacketV5.from(buffer) as PublishComplete
        val (key, value) = requestRead.variable.properties.userProperty.first()
        assertEquals(key.toString(), "key")
        assertEquals(value.toString(), "value")
        assertEquals(request, requestRead)
    }

}
