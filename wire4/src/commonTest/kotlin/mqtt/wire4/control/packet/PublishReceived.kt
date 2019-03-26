@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlin.test.Test
import kotlin.test.assertEquals

class PublishReceivedTests {
    private val packetIdentifier = 2.toUShort()

    @Test
    fun packetIdentifier() {
        val puback = PublishReceived(packetIdentifier)
        val data = puback.serialize()
        val pubackResult = ControlPacketV4.from(data) as PublishReceived
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
    }

    @Test
    fun packetIdentifierSendDefaults() {
        val puback = PublishReceived(packetIdentifier)
        val data = puback.serialize(true)
        val pubackResult = ControlPacketV4.from(data) as PublishReceived
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
    }
}
