package mqtt.wire4.control.packet

import mqtt.wire.control.packet.IReserved
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow


object Reserved : ControlPacketV4(0, DirectionOfFlow.FORBIDDEN), IReserved
