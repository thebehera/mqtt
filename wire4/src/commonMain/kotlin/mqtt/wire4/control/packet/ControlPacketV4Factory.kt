@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import mqtt.buffer.GenericType
import mqtt.buffer.ReadBuffer
import mqtt.wire.control.packet.ControlPacketFactory
import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.control.packet.SubscriptionWrapper
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService

object ControlPacketV4Factory : ControlPacketFactory {
    override fun from(buffer: ReadBuffer, byte1: UByte, remainingLength: UInt) =
        ControlPacketV4.from(buffer, byte1, remainingLength)

    override fun pingRequest() = PingRequest
    override fun pingResponse() = PingResponse

    override fun subscribe(
        packetIdentifier: Int,
        subscriptions: Set<SubscriptionWrapper>,
        // Below are only used for mqtt 5, ignored on versions below
        reasonString: CharSequence?,
        userProperty: List<Pair<CharSequence, CharSequence>>
    ) = SubscribeRequest(packetIdentifier, subscriptions.map { Subscription(it.topicFilter, it.maximumQos) })

    override fun unsubscribe(packetIdentifier: Int, subscriptions: Set<CharSequence>) =
        UnsubscribeRequest(packetIdentifier, subscriptions.map { MqttUtf8String(it) })

    override fun <ApplicationMessage : Any, CorrelationData : Any> publish(
        dup: Boolean,
        qos: QualityOfService,
        packetIdentifier: Int?,
        retain: Boolean,
        topicName: CharSequence,
        payload: GenericType<ApplicationMessage>?,
        // MQTT 5 Properties, Should be ignored in this version
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
        val variableHeader = PublishMessage.VariableHeader(topicName, packetIdentifier)
        return PublishMessage(fixedHeader, variableHeader, payload)
    }

    override fun reserved() = Reserved
    override fun disconnect(
        reasonCode: ReasonCode,
        sessionExpiryIntervalSeconds: Long?,
        reasonString: CharSequence?,
        userProperty: List<Pair<CharSequence, CharSequence>>,
        serverReference: CharSequence?
    ) = DisconnectNotification
}