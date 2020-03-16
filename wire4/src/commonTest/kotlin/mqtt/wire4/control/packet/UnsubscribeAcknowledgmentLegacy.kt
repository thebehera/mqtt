@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlin.test.Test
import kotlin.test.assertEquals

class UnsubscribeAcknowledgmentTestsLegacy {
    private val packetIdentifier = 2

    @Test
    fun serializeDeserializeDefault() {
        val actual = UnsubscribeAcknowledgment(packetIdentifier)
        val bytes = actual.serialize()
        val expected = ControlPacketV4.from(bytes)
        assertEquals(expected, actual)
    }
}
