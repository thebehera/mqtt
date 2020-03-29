@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.buffer.ReadBuffer
import mqtt.wire.control.packet.ControlPacketReader

@Parcelize
object ControlPacketV4Reader : ControlPacketReader, Parcelable {
    override fun from(buffer: ReadBuffer, byte1: UByte, remainingLength: UInt) =
        ControlPacketV4.from(buffer, byte1, remainingLength)
}