@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class ResponseTopic(val value: CharSequence) : Property(0x08, Type.UTF_8_ENCODED_STRING, willProperties = true) {
    override fun write(buffer: WriteBuffer) = write(buffer, value)
    override fun size() = size(value)
}
