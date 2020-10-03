@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class ServerReference(val otherServer: CharSequence) : Property(0x1C, Type.UTF_8_ENCODED_STRING) {
    override fun write(buffer: WriteBuffer) = write(buffer, otherServer)
    override fun size() = size(otherServer)
}
