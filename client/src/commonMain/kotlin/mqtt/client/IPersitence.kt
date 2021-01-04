package mqtt.client

import kotlinx.coroutines.Deferred
import mqtt.connection.IConnectionOptions
import mqtt.wire.control.packet.*
import mqtt.wire.data.QualityOfService

typealias MqttConnectionId = Long

interface MqttConnections {
    fun create(connection: IConnectionOptions): MqttOperation<IConnectionAcknowledgment>
    fun read(): Map<MqttConnectionId, IConnectionOptions>
    fun delete(connectionId: MqttConnectionId): MqttOperation<IDisconnectNotification>
}

interface MqttQueue

interface MqttOperation<FinalAck : ControlPacket> {
    val persistenceUpdated: Deferred<MqttConnectionId>
    val response: Deferred<FinalAck?>
}

sealed class MqttPublishOperation {
    abstract val persistenceUpdated: Deferred<MqttConnectionId>

    class Qos1(override val persistenceUpdated: Deferred<MqttConnectionId>, val ack: Deferred<IPublishAcknowledgment>) :
        MqttPublishOperation()

    class Qos2(
        override val persistenceUpdated: Deferred<MqttConnectionId>,
        val received: Deferred<IPublishReceived>,
        val complete: Deferred<IPublishComplete>
    ) : MqttPublishOperation()
}


interface MqttApi {
    fun subscribe(
        subscription: CharSequence,
        maxQos: QualityOfService = QualityOfService.EXACTLY_ONCE,
        // The rest of these properties are used by mqtt 5
        noLocal: Boolean = false,
        retainAsPublished: Boolean = false,
        retainHandling: RetainHandling = RetainHandling.SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE,
        reasonString: CharSequence? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList()
    ): MqttOperation<ISubscribeAcknowledgement>

    fun unsubscribe(vararg subscriptions: CharSequence): MqttOperation<IUnsubscribeAckowledgment>

    fun publish(
        topicName: CharSequence,
        payload: String? = null,
        qos: QualityOfService = QualityOfService.AT_LEAST_ONCE,
        dup: Boolean = false,
        retain: Boolean = false,
        // MQTT 5 Properties
        payloadFormatIndicator: Boolean = false,
        messageExpiryInterval: Long? = null,
        topicAlias: Int? = null,
        responseTopic: CharSequence? = null,
        correlationData: String? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList(),
        subscriptionIdentifier: Set<Long> = emptySet(),
        contentType: CharSequence? = null
    ): MqttPublishOperation?

}