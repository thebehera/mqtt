package mqtt.wire4.control.packet

import mqtt.Parcelize
import mqtt.wire.control.packet.IReserved
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

@Parcelize
object Reserved : ControlPacketV4(0, DirectionOfFlow.FORBIDDEN), IReserved
