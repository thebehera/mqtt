package mqtt.wire4.control.packet

import mqtt.wire.control.packet.IPingResponse
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

object PingResponse : ControlPacketV4(13, DirectionOfFlow.SERVER_TO_CLIENT), IPingResponse
