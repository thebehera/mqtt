@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import mqtt.buffer.ReadBuffer
import mqtt.wire.buffer.GenericType
import mqtt.wire.buffer.readVariableByteInteger
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.ReasonCode.NORMAL_DISCONNECTION
import mqtt.wire.data.QualityOfService

interface ControlPacketFactory {
    fun from(buffer: ReadBuffer): ControlPacket {
        val byte1 = buffer.readUnsignedByte()
        val remainingLength = buffer.readVariableByteInteger()
        return from(buffer, byte1, remainingLength)
    }

    fun from(buffer: ReadBuffer, byte1: UByte, remainingLength: UInt): ControlPacket

    fun pingRequest(): IPingRequest
    fun pingResponse(): IPingResponse

    fun subscribe(
        packetIdentifier: Int,
        subscriptions: Set<SubscriptionWrapper>,
        reasonString: CharSequence? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList()
    ): ISubscribeRequest

    fun unsubscribe(
        packetIdentifier: Int,
        subscriptions: Set<CharSequence>
    ): IUnsubscribeRequest

    fun publish(
        dup: Boolean = false,
        qos: QualityOfService = QualityOfService.EXACTLY_ONCE,
        packetIdentifier: Int? = null,
        retain: Boolean = false,
        topicName: CharSequence,
        payload: String? = null,
        // MQTT 5 Properties
        payloadFormatIndicator: Boolean = false,
        messageExpiryInterval: Long? = null,
        topicAlias: Int? = null,
        responseTopic: CharSequence? = null,
        correlationData: String? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList(),
        subscriptionIdentifier: Set<Long> = emptySet(),
        contentType: CharSequence? = null
    ): IPublishMessage {
        return publish(
            dup,
            qos,
            packetIdentifier,
            retain,
            topicName,
            payload?.let { GenericType(it, String::class) },
            payloadFormatIndicator,
            messageExpiryInterval,
            topicAlias,
            responseTopic,
            correlationData?.let { GenericType(it, String::class) },
            userProperty,
            subscriptionIdentifier,
            contentType
        )
    }

    fun <ApplicationMessage : Any, CorrelationData : Any> publish(
        dup: Boolean = false,
        qos: QualityOfService = QualityOfService.EXACTLY_ONCE,
        packetIdentifier: Int? = null,
        retain: Boolean = false,
        topicName: CharSequence,
        payload: GenericType<ApplicationMessage>? = null,
        // MQTT 5 Properties
        payloadFormatIndicator: Boolean = false,
        messageExpiryInterval: Long? = null,
        topicAlias: Int? = null,
        responseTopic: CharSequence? = null,
        correlationData: GenericType<CorrelationData>? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList(),
        subscriptionIdentifier: Set<Long> = emptySet(),
        contentType: CharSequence? = null
    ): IPublishMessage


    fun reserved(): IReserved
    fun disconnect(
        reasonCode: ReasonCode = NORMAL_DISCONNECTION,
        sessionExpiryIntervalSeconds: Long? = null,
        reasonString: CharSequence? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList(),
        serverReference: CharSequence? = null
    ): IDisconnectNotification

}