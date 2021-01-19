@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet


import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class PublishCompleteTests {
    private val packetIdentifier = 2

    @Test
    fun packetIdentifier() {
        val puback = PublishComplete(packetIdentifier)
        val buffer = allocateNewBuffer(4u)
        puback.serialize(buffer)
        buffer.resetForRead()
        val pubackResult = ControlPacketV4.from(buffer) as PublishComplete
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
    }
}
