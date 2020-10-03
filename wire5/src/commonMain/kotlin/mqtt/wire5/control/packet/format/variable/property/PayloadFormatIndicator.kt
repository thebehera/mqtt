@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class PayloadFormatIndicator(val willMessageIsUtf8: Boolean) : Property(
    0x01, Type.BYTE,
    willProperties = true
) {
    override fun size() = 2u
    override fun write(buffer: WriteBuffer) = write(buffer, willMessageIsUtf8)
}
