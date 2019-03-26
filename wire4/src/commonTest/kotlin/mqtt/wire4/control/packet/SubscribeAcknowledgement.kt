@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.wire4.control.packet.SubscribeAcknowledgement.ReturnCode.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscribeAcknowledgementTests {
    private val packetIdentifier = 2.toUShort()

    @Test
    fun successMaxQos0() {
        val payload = SUCCESS_MAXIMUM_QOS_0
        val puback = SubscribeAcknowledgement(packetIdentifier, listOf(payload))
        val data = puback.serialize()
        val pubackResult = ControlPacket.from(data) as SubscribeAcknowledgement
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
        assertEquals(pubackResult.payload, listOf(SUCCESS_MAXIMUM_QOS_0))
    }

    @Test
    fun grantedQos1() {
        val payload = SUCCESS_MAXIMUM_QOS_1
        val puback = SubscribeAcknowledgement(packetIdentifier, listOf(payload))
        val data = puback.serialize()
        val pubackResult = ControlPacket.from(data) as SubscribeAcknowledgement
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
        assertEquals(pubackResult.payload, listOf(SUCCESS_MAXIMUM_QOS_1))

    }

    @Test
    fun grantedQos2() {
        val payload = SUCCESS_MAXIMUM_QOS_2
        val puback = SubscribeAcknowledgement(packetIdentifier, listOf(payload))
        val data = puback.serialize()
        val pubackResult = ControlPacket.from(data) as SubscribeAcknowledgement
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
        assertEquals(pubackResult.payload, listOf(SUCCESS_MAXIMUM_QOS_2))
    }

    @Test
    fun failure() {
        val payload = FAILURE
        val puback = SubscribeAcknowledgement(packetIdentifier, listOf(payload))
        val data = puback.serialize()
        val pubackResult = ControlPacket.from(data) as SubscribeAcknowledgement
        assertEquals(pubackResult.packetIdentifier, packetIdentifier)
        assertEquals(pubackResult.payload, listOf(FAILURE))
    }
}
