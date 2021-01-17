@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import mqtt.buffer.ReadBuffer
import mqtt.wire.buffer.GenericType
import mqtt.wire.control.packet.*
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.data.QualityOfService
import mqtt.wire5.control.packet.DisconnectNotification.VariableHeader

object ControlPacketV5Factory : ControlPacketFactory {
    override fun from(buffer: ReadBuffer, byte1: UByte, remainingLength: UInt) =
        ControlPacketV5.from(buffer, byte1, remainingLength)


    override fun pingRequest() = PingRequest
    override fun pingResponse() = PingResponse


    override fun subscribe(
        packetIdentifier: Int,
        subscriptions: Set<SubscriptionWrapper>,
        reasonString: CharSequence?,
        userProperty: List<Pair<CharSequence, CharSequence>>
    ): ISubscribeRequest {
        val variableHeader = SubscribeRequest.VariableHeader(
            packetIdentifier,
            SubscribeRequest.VariableHeader.Properties(reasonString, userProperty)
        )
        val subscriptionsMqtt5 = subscriptions.map {
            Subscription(
                it.topicFilter,
                it.maximumQos,
                it.noLocal,
                it.retainAsPublished,
                it.retainHandling
            )
        }
        return SubscribeRequest(variableHeader, subscriptionsMqtt5.toSet())
    }

    override fun unsubscribe(packetIdentifier: Int, subscriptions: Set<CharSequence>) =
        UnsubscribeRequest(packetIdentifier, subscriptions)

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

    override fun reserved() = Reserved
    override fun disconnect(
        reasonCode: ReasonCode,
        sessionExpiryIntervalSeconds: Long?,
        reasonString: CharSequence?,
        userProperty: List<Pair<CharSequence, CharSequence>>,
        serverReference: CharSequence?
    ): IDisconnectNotification {
        return DisconnectNotification(
            VariableHeader(
                reasonCode,
                VariableHeader.Properties(sessionExpiryIntervalSeconds, reasonString, userProperty, serverReference)
            )
        )
    }
}