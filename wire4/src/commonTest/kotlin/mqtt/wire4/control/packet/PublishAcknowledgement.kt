@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet


import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class PublishAcknowledgementTest {
    private val packetIdentifier = 2

    @Test
    fun packetIdentifier() {
        val buffer = allocateNewBuffer(4u)
        val puback = PublishAcknowledgment(packetIdentifier)
        puback.serialize(buffer)
        buffer.resetForRead()
        val pubackResult = ControlPacketV4.from(buffer) as PublishAcknowledgment
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
    }

    @Test
    fun packetIdentifierSendDefaults() {
        val buffer = allocateNewBuffer(4u)
        val puback = PublishAcknowledgment(packetIdentifier)
        puback.serialize(buffer)
        buffer.resetForRead()
        val pubackResult = ControlPacketV4.from(buffer) as PublishAcknowledgment
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
    }

}
