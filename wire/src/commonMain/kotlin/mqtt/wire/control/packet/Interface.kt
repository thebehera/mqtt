package mqtt.wire.control.packet

import mqtt.wire.control.packet.fixed.FixedHeader
import mqtt.wire.control.packet.variable.VariableHeader

interface Interface {
    val fixedHeader: FixedHeader
    val variableHeader: VariableHeader? get() = null
    val payload: ByteArray

}

