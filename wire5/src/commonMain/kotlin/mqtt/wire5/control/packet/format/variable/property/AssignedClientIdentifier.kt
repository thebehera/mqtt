@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class AssignedClientIdentifier(val value: CharSequence) : Property(0x12, Type.UTF_8_ENCODED_STRING) {
    override fun write(buffer: WriteBuffer) = write(buffer, value)
    override fun size() = size(value)
}
