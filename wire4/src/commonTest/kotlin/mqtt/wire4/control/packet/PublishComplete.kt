@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlin.test.Test
import kotlin.test.assertEquals

class PublishCompleteTests {
    private val packetIdentifier = 2.toUShort()

    @Test
    fun packetIdentifier() {
        val puback = PublishComplete(packetIdentifier)
        val data = puback.serialize()
        val pubackResult = ControlPacket.from(data) as PublishComplete
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
    }
}
