@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class TopicAlias(val value: Int) : Property(0x22, Type.TWO_BYTE_INTEGER) {
    override fun size(buffer: WriteBuffer) = 3u
    override fun write(buffer: WriteBuffer) = write(buffer, value.toUShort())
}
