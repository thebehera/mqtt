package mqtt.wire5.control.packet

import mqtt.Parcelize
import mqtt.wire.control.packet.IReserved
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

@Parcelize
object Reserved : ControlPacketV5(0, DirectionOfFlow.FORBIDDEN), IReserved
