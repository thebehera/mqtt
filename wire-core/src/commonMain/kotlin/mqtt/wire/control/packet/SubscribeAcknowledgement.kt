@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

interface ISubscribeAcknowledgement : ControlPacket {
    val packetIdentifier: Int
}