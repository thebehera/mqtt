@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

data class PublishReceived(val packetIdentifier: UShort) : ControlPacket(5, DirectionOfFlow.BIDIRECTIONAL)
