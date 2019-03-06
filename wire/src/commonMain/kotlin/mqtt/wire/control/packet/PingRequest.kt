package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

object PingRequest : ControlPacket(12, DirectionOfFlow.CLIENT_TO_SERVER)
