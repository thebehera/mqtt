@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

data class SubscribeAcknowledgment(val packetIdentifier: UShort) : ControlPacket(9, DirectionOfFlow.SERVER_TO_CLIENT)
