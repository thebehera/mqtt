package mqtt.wire5.control.packet

import mqtt.wire.control.packet.IReserved
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

object Reserved : ControlPacketV5(0, DirectionOfFlow.FORBIDDEN), IReserved
