package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

object AuthenticationExchange : ControlPacket(15, DirectionOfFlow.BIDIRECTIONAL)
