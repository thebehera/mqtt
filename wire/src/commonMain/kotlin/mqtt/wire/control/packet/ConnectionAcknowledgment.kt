package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

object ConnectionAcknowledgment : ControlPacket(2, DirectionOfFlow.SERVER_TO_CLIENT)
