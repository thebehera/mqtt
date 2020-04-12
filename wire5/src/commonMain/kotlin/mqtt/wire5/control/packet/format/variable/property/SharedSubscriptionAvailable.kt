@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class SharedSubscriptionAvailable(val serverSupported: Boolean) : Property(0x2A, Type.BYTE) {
    override fun size(buffer: WriteBuffer) = 2u
    override fun write(buffer: WriteBuffer) = write(buffer, serverSupported)
}
