@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.wire.control.packet.format.ReasonCode.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscribeAcknowledgementTestsLegacy {
    private val packetIdentifier = 2

    @Test
    fun successMaxQos0() {
        val payload = GRANTED_QOS_0
        val puback = SubscribeAcknowledgement(packetIdentifier, listOf(payload))
        val data = puback.serialize()
        val pubackResult = ControlPacketV4.from(data) as SubscribeAcknowledgement
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
        assertEquals(pubackResult.payload, listOf(GRANTED_QOS_0))
    }

    @Test
    fun grantedQos1() {
        val payload = GRANTED_QOS_1
        val puback = SubscribeAcknowledgement(packetIdentifier, listOf(payload))
        val data = puback.serialize()
        val pubackResult = ControlPacketV4.from(data) as SubscribeAcknowledgement
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
        assertEquals(pubackResult.payload, listOf(GRANTED_QOS_1))

    }

    @Test
    fun grantedQos2() {
        val payload = GRANTED_QOS_2
        val puback = SubscribeAcknowledgement(packetIdentifier, listOf(payload))
        val data = puback.serialize()
        val pubackResult = ControlPacketV4.from(data) as SubscribeAcknowledgement
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
        assertEquals(pubackResult.payload, listOf(GRANTED_QOS_2))
    }

    @Test
    fun failure() {
        val payload = UNSPECIFIED_ERROR
        val puback = SubscribeAcknowledgement(packetIdentifier, listOf(payload))
        val data = puback.serialize()
        val pubackResult = ControlPacketV4.from(data) as SubscribeAcknowledgement
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
        assertEquals(pubackResult.payload, listOf(UNSPECIFIED_ERROR))
    }
}
