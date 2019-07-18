@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import mqtt.wire.data.topic.Filter

interface ISubscribeRequest : ControlPacket {
    val packetIdentifier: UShort
    fun expectedResponse(): ISubscribeAcknowledgement
    fun getTopics(): List<Filter>
}
