@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class DisconnectTests {
    @Test
    fun serializeDeserialize() {
        val actual = DisconnectNotification
        val buffer = allocateNewBuffer(2u, limits)
        actual.serialize(buffer)
        buffer.resetForRead()
        val expected = ControlPacketV4.from(buffer) as DisconnectNotification
        assertEquals(expected, actual)
    }
}
