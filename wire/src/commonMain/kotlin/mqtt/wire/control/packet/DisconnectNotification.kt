package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

object DisconnectNotification : ControlPacket(14, DirectionOfFlow.BIDIRECTIONAL)
