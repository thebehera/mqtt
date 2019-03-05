@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

data class PublishRelease(val packetIdentifier: UShort) : ControlPacket(6, DirectionOfFlow.BIDIRECTIONAL, 0b10)
