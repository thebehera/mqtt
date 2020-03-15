@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlin.test.Test
import kotlin.test.assertEquals

class PublishCompleteTestsLegacy {
    private val packetIdentifier = 2

    @Test
    fun packetIdentifier() {
        val puback = PublishComplete(packetIdentifier)
        val data = puback.serialize()
        val pubackResult = ControlPacketV4.from(data) as PublishComplete
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
    }
}