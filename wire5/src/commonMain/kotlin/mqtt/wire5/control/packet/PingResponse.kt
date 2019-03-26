package mqtt.wire5.control.packet

import mqtt.wire.control.packet.IPingResponse
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

object PingResponse : ControlPacketV5(13, DirectionOfFlow.SERVER_TO_CLIENT), IPingResponse
