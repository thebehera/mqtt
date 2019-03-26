package mqtt.wire5.control.packet

import mqtt.wire5.control.packet.format.fixed.DirectionOfFlow

object Reserved : ControlPacket(0, DirectionOfFlow.FORBIDDEN)
