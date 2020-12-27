package mqtt

import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.control.packet.ISubscribeAcknowledgement
import mqtt.wire.control.packet.IUnsubscribeAckowledgment
import mqtt.wire.control.packet.format.ReasonCode
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
interface ApplicationMessageCallback {
    suspend fun onPublishMessageReceived5(client: Client, pub: IPublishMessage): Mqtt5Extras? {
        onPublishMessageReceived(client, pub)
        return null
    }

    suspend fun onPublishMessageReceived(client: Client, pub: IPublishMessage) = Unit
    suspend fun onAcknowledgementReceived(ack: ISubscribeAcknowledgement) = Unit
    suspend fun onAcknowledgementReceived(ack: IUnsubscribeAckowledgment) = Unit

    class Mqtt5Extras(
        val reasonCode: ReasonCode = ReasonCode.SUCCESS,
        val reasonString: CharSequence? = null,
        val userProperty: List<Pair<CharSequence, CharSequence>> = emptyList()
    )
}