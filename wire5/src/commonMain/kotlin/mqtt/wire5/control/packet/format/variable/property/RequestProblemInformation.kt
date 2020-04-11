@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class RequestProblemInformation(val reasonStringOrUserPropertiesAreSentInFailures: Boolean) :
    Property(0x17, Type.BYTE) {
    override fun size(buffer: WriteBuffer) = 2u
    override fun write(buffer: WriteBuffer) = write(buffer, reasonStringOrUserPropertiesAreSentInFailures)
}
