@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

interface IPublishRelease : ControlPacket {
    val packetIdentifier: Int
    fun expectedResponse(): IPublishComplete
}