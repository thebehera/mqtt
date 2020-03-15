@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlin.test.Test
import kotlin.test.assertEquals

class PublishReleaseTestsLegacy {
    private val packetIdentifier = 2

    @Test
    fun packetIdentifier() {
        val puback = PublishRelease(packetIdentifier)
        val data = puback.serialize()
        val pubackResult = ControlPacketV4.from(data) as PublishRelease
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
    }
}
