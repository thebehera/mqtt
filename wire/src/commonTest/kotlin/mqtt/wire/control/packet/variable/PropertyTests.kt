package mqtt.wire.control.packet.variable

import mqtt.wire.control.packet.fixed.ControlPacketType.PUBLISH
import mqtt.wire.control.packet.variable.Property.*
import mqtt.wire.data.Type.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyTests {
    @Test
    fun payloadFormatIndicatorIdentifier() =
            assertEquals(0x01, PAYLOAD_FORMAT_INDICATOR.identifier,
                    "invalid identifier: payload format indicator hex")

    @Test
    fun payloadFormatIndicatorType() =
            assertEquals(BYTE, PAYLOAD_FORMAT_INDICATOR.type,
                    "invalid type: payload format indicator")

    @Test
    fun payloadFormatIndicatorPacketPublish() =
            assertTrue(PAYLOAD_FORMAT_INDICATOR.packetTypes.contains(PUBLISH),
                    "missing control packet type: $PUBLISH")

    @Test
    fun payloadFormatIndicatorCorrectControlTypeCount() =
            assertEquals(PAYLOAD_FORMAT_INDICATOR.packetTypes.size, 1, "invalid control type count")

    @Test
    fun payloadFormatIndicatorPacketWillProperties() =
            assertTrue(PAYLOAD_FORMAT_INDICATOR.willProperties, "incorrect will property")

    @Test
    fun messageExpiryIntervalIdentifier() =
            assertEquals(0x01, MESSAGE_EXPIRY_INTERVAL.identifier,
                    "invalid identifier: message expiry interval hex")

    @Test
    fun messageExpiryIntervalType() =
            assertEquals(FOUR_BYTE_INTEGER, MESSAGE_EXPIRY_INTERVAL.type,
                    "invalid type: message expiry interval")

    @Test
    fun messageExpiryIntervalPacketPublish() =
            assertTrue(MESSAGE_EXPIRY_INTERVAL.packetTypes.contains(PUBLISH),
                    "missing control packet type: $PUBLISH")

    @Test
    fun messageExpiryIntervalCorrectControlTypeCount() =
            assertEquals(MESSAGE_EXPIRY_INTERVAL.packetTypes.size, 1, "invalid control type count")

    @Test
    fun messageExpiryIntervalPacketWillProperties() =
            assertTrue(MESSAGE_EXPIRY_INTERVAL.willProperties, "incorrect will property")

    @Test
    fun contentTypeIdentifier() =
            assertEquals(0x01, CONTENT_TYPE.identifier,
                    "invalid identifier: content type hex")

    @Test
    fun contentTypeType() =
            assertEquals(UTF_8_ENCODED_STRING, CONTENT_TYPE.type,
                    "invalid type: content type")

    @Test
    fun contentTypePacketPublish() =
            assertTrue(CONTENT_TYPE.packetTypes.contains(PUBLISH),
                    "missing control packet type: $PUBLISH")

    @Test
    fun contentTypeCorrectControlTypeCount() =
            assertEquals(CONTENT_TYPE.packetTypes.size, 1, "invalid control type count")

    @Test
    fun contentTypePacketWillProperties() =
            assertTrue(CONTENT_TYPE.willProperties, "incorrect will property")

}