package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

object PingResponse : ControlPacket(13, DirectionOfFlow.SERVER_TO_CLIENT)
