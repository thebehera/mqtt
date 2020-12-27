@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import mqtt.wire.control.packet.RetainHandling.SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Filter

interface ISubscribeRequest : ControlPacket {
    val packetIdentifier: Int
    fun expectedResponse(): ISubscribeAcknowledgement
    fun getTopics(): List<Filter>

    companion object {
        const val controlPacketValue: Byte = 8
    }
}


class SubscriptionWrapper(
    val topicFilter: Filter,
    val maximumQos: QualityOfService = QualityOfService.AT_LEAST_ONCE,
    val noLocal: Boolean = false,
    val retainAsPublished: Boolean = false,
    val retainHandling: RetainHandling = SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE
)

enum class RetainHandling(val value: UByte) {
    SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE(0.toUByte()),
    SEND_RETAINED_MESSAGES_AT_SUBSCRIBE_ONLY_IF_SUBSCRIBE_DOESNT_EXISTS(1.toUByte()),
    DO_NOT_SEND_RETAINED_MESSAGES(2.toUByte())
}