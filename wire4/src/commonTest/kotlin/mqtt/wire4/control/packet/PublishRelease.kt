@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlin.test.Test
import kotlin.test.assertEquals

class PublishReleaseTests {
    private val packetIdentifier = 2.toUShort()

    @Test
    fun packetIdentifier() {
        val puback = PublishRelease(packetIdentifier)
        val data = puback.serialize()
        val pubackResult = ControlPacket.from(data) as PublishRelease
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
    }
}
