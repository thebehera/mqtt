package mqtt.wire.control.packet

import kotlinx.io.core.IoBuffer

interface Interface {
    val fixedHeader: FixedHeader
    //    val variableHeader :VariableHeader
    val payload: IoBuffer
}

