@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import mqtt.buffer.GenericType
import mqtt.buffer.ReadBuffer
import mqtt.wire.control.packet.ControlPacketFactory
import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.data.QualityOfService

object ControlPacketV5Factory : ControlPacketFactory {
    override fun from(buffer: ReadBuffer, byte1: UByte, remainingLength: UInt) =
        ControlPacketV5.from(buffer, byte1, remainingLength)


    override fun pingRequest() = PingRequest
    override fun pingResponse() = PingResponse


    override fun <ApplicationMessage : Any, CorrelationData : Any> publish(
        dup: Boolean,
        qos: QualityOfService,
        packetIdentifier: Int?,
        retain: Boolean,
        topicName: CharSequence,
        payload: GenericType<ApplicationMessage>?,
        payloadFormatIndicator: Boolean,
        messageExpiryInterval: Long?,
        topicAlias: Int?,
        responseTopic: CharSequence?,
        correlationData: GenericType<CorrelationData>?,
        userProperty: List<Pair<CharSequence, CharSequence>>,
        subscriptionIdentifier: Set<Long>,
        contentType: CharSequence?
    ): IPublishMessage {
        val fixedHeader = PublishMessage.FixedHeader(dup, qos, retain)
        val properties = PublishMessage.VariableHeader.Properties(
            payloadFormatIndicator,
            messageExpiryInterval,
            topicAlias,
            responseTopic,
            correlationData,
            userProperty,
            subscriptionIdentifier,
            contentType
        )
        val variableHeader = PublishMessage.VariableHeader(topicName, packetIdentifier, properties)
        return PublishMessage(fixedHeader, variableHeader, payload)
    }
}