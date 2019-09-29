@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

interface IPublishAcknowledgment : ControlPacket {
    val packetIdentifier: Int
}