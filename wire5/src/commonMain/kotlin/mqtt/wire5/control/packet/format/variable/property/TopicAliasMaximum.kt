@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class TopicAliasMaximum(val highestValueSupported: Int) : Property(0x23, Type.TWO_BYTE_INTEGER) {
    override fun size() = 3u
    override fun write(buffer: WriteBuffer) = write(buffer, highestValueSupported.toUShort())
}
