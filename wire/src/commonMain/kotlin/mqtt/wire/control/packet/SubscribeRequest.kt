@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

data class SubscribeRequest(val packetIdentifier: UShort) : ControlPacket(8, DirectionOfFlow.CLIENT_TO_SERVER, 0b10)
