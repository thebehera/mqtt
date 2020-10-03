@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet.format.variable.property

import mqtt.buffer.WriteBuffer
import mqtt.wire.data.Type

data class ResponseInformation(val requestResponseInformationInConnack: CharSequence) :
    Property(0x1A, Type.UTF_8_ENCODED_STRING) {
    override fun write(buffer: WriteBuffer) = write(buffer, requestResponseInformationInConnack)
    override fun size() = size(requestResponseInformationInConnack)
}
