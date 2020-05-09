@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class SessionExpiryInterval(val seconds: Long) : Property(0x11, Type.FOUR_BYTE_INTEGER) {
    override fun size(buffer: WriteBuffer) = size(buffer, seconds.toUInt())
    override fun write(buffer: WriteBuffer) = write(buffer, seconds.toUInt())
}
