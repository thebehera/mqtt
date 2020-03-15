@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlin.test.Test
import kotlin.test.assertEquals

class DisconnectTestsLegacy {
    @Test
    fun serializeDeserialize() {
        val actual = DisconnectNotification
        val bytes = actual.serialize()
        val expected = ControlPacketV4.from(bytes) as DisconnectNotification
        assertEquals(expected, actual)
    }
}