@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet.format.variable

import mqtt.wire.control.packet.format.variable.Property.*
import mqtt.wire.data.Type.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyTests {
    @Test
    fun payloadFormatIndicatorIdentifier() =
            assertEquals(0x01, PAYLOAD_FORMAT_INDICATOR.identifier.value.toInt(),
                    "invalid identifier: payload format indicator hex")

    @Test
    fun payloadFormatIndicatorType() =
            assertEquals(BYTE, PAYLOAD_FORMAT_INDICATOR.type,
                    "invalid type: payload format indicator")

    @Test
    fun payloadFormatIndicatorPacketWillProperties() =
            assertTrue(PAYLOAD_FORMAT_INDICATOR.willProperties, "incorrect will property")

    @Test
    fun messageExpiryIntervalIdentifier() =
            assertEquals(0x02, MESSAGE_EXPIRY_INTERVAL.identifier.value.toInt(),
                    "invalid identifier: message expiry interval hex")

    @Test
    fun messageExpiryIntervalType() =
            assertEquals(FOUR_BYTE_INTEGER, MESSAGE_EXPIRY_INTERVAL.type,
                    "invalid type: message expiry interval")

    @Test
    fun messageExpiryIntervalPacketWillProperties() =
            assertTrue(MESSAGE_EXPIRY_INTERVAL.willProperties, "incorrect will property")

    @Test
    fun contentTypeType() =
            assertEquals(UTF_8_ENCODED_STRING, CONTENT_TYPE.type,
                    "invalid type: content type")
    @Test
    fun contentTypePacketWillProperties() =
            assertTrue(CONTENT_TYPE.willProperties, "incorrect will property")

}
