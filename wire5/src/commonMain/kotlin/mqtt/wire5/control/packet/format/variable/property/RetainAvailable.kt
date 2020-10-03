@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class RetainAvailable(val serverSupported: Boolean) : Property(0x25, Type.BYTE) {
    override fun size() = 2u
    override fun write(buffer: WriteBuffer) = write(buffer, serverSupported)
}
