@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlin.test.Test
import kotlin.test.assertEquals

class DisconnectTests {
    @Test
    fun serializeDeserialize() {
        val actual = DisconnectNotification
        val bytes = actual.serialize()
        val expected = ControlPacket.from(bytes) as DisconnectNotification
        assertEquals(expected, actual)
    }
}
