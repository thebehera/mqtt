package mqtt.wire4.control.packet

import mqtt.Parcelize
import mqtt.wire.control.packet.IPingResponse
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

@Parcelize
object PingResponse : ControlPacketV4(13, DirectionOfFlow.SERVER_TO_CLIENT), IPingResponse
