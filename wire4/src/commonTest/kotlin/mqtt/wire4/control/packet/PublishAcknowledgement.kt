@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlin.test.Test
import kotlin.test.assertEquals

class PublishAcknowledgementTest {
    private val packetIdentifier = 2.toUShort()

    @Test
    fun packetIdentifier() {
        val puback = PublishAcknowledgment(packetIdentifier)
        val data = puback.serialize()
        val pubackResult = ControlPacket.from(data) as PublishAcknowledgment
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
    }

    @Test
    fun packetIdentifierSendDefaults() {
        val puback = PublishAcknowledgment(packetIdentifier)
        val data = puback.serialize(true)
        val pubackResult = ControlPacket.from(data) as PublishAcknowledgment
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
    }

}
