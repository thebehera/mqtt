@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

interface IPublishReceived : ControlPacket {
    val packetIdentifier: Int
    fun expectedResponse(): IPublishRelease
}