@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

interface IPublishComplete : ControlPacket {
    val packetIdentifier: UShort
}