package mqtt.wire.control.packet.format

import mqtt.wire.control.packet.format.fixed.FixedHeader
import mqtt.wire.control.packet.format.variable.VariableHeader

interface Interface {
    val fixedHeader: FixedHeader
    val variableHeader: VariableHeader? get() = null
    val payload: ByteArray

}

