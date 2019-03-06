package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

object Reserved : ControlPacket(0, DirectionOfFlow.FORBIDDEN)
