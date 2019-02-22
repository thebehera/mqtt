package mqtt.wire.control.packet

import kotlinx.io.core.IoBuffer
import mqtt.wire.control.packet.fixed.FixedHeader

interface Interface {
    val fixedHeader: FixedHeader
    //    val variableHeader :VariableHeader
    val payload: IoBuffer
}

