package mqtt.wire5.control.packet

import mqtt.wire5.control.packet.format.fixed.DirectionOfFlow

object PingResponse : ControlPacket(13, DirectionOfFlow.SERVER_TO_CLIENT)
