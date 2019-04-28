@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

interface IPublishReceived : ControlPacket {
    val packetIdentifier: UShort
    fun expectedResponse(): IPublishRelease
}