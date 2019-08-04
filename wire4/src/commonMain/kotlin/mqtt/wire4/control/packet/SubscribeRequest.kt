@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.*
import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.wire.control.packet.ISubscribeRequest
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.control.packet.getAndIncrementPacketIdentifier
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.*
import mqtt.wire.data.readMqttFilter
import mqtt.wire.data.topic.Filter
import mqtt.wire.data.writeMqttFilter

/**
 * 3.8 SUBSCRIBE - Subscribe request
 *
 * The SUBSCRIBE packet is sent from the Client to the Server to create one or more Subscriptions. Each Subscription
 * registers a Client’s interest in one or more Topics. The Server sends PUBLISH packets to the Client to forward
 * Application Messages that were published to Topics that match these Subscriptions. The SUBSCRIBE packet also
 * specifies (for each Subscription) the maximum QoS with which the Server can send Application Messages to the Client.
 *
 * Bits 3,2,1 and 0 of the Fixed Header of the SUBSCRIBE packet are reserved and MUST be set to 0,0,1 and 0
 * respectively. The Server MUST treat any other value as malformed and close the Network Connection [MQTT-3.8.1-1].
 */
@Parcelize
data class SubscribeRequest(override val packetIdentifier: UShort = getAndIncrementPacketIdentifier(),
                            val subscriptions: List<Subscription>)
    : ControlPacketV4(8, DirectionOfFlow.CLIENT_TO_SERVER, 0b10), ISubscribeRequest {

    constructor(topic: Filter, qos: QualityOfService)
            : this(subscriptions = listOf(Subscription(topic, qos)))


    constructor(packetIdentifier: UShort = getAndIncrementPacketIdentifier(), topic: Filter, qos: QualityOfService)
            : this(packetIdentifier, listOf(Subscription(topic, qos)))


    constructor(topics: List<Filter>, qos: List<QualityOfService>)
            : this(subscriptions = Subscription.from(topics, qos))
    private val payloadSubs by lazy { Subscription.writeMany(subscriptions) }
    override val variableHeaderPacket = buildPacket {
        writeUShort(packetIdentifier)
    }

    override fun payloadPacket(sendDefaults: Boolean) = payloadSubs

    override fun expectedResponse(): SubscribeAcknowledgement {
        val returnCodes = subscriptions.map {
            when (it.maximumQos) {
                AT_MOST_ONCE -> ReasonCode.GRANTED_QOS_0
                AT_LEAST_ONCE -> ReasonCode.GRANTED_QOS_1
                EXACTLY_ONCE -> ReasonCode.GRANTED_QOS_2
            }
        }
        return SubscribeAcknowledgement(packetIdentifier, returnCodes)
    }

    override fun getTopics() = subscriptions.map { it.topicFilter }

    companion object {
        fun from(buffer: ByteReadPacket): SubscribeRequest {
            val packetIdentifier = buffer.readUShort()
            val subscriptions = Subscription.fromMany(buffer)
            return SubscribeRequest(packetIdentifier, subscriptions)
        }
    }
}

@Parcelize
data class Subscription(val topicFilter: Filter,
                        /**
                         * Bits 0 and 1 of the Subscription Options represent Maximum QoS field. This gives the maximum
                         * QoS level at which the Server can send Application Messages to the Client. It is a Protocol
                         * Error if the Maximum QoS field has the value 3.
                         */
                        val maximumQos: QualityOfService = AT_LEAST_ONCE
) : Parcelable {
    val packet by lazy {
        val qosInt = maximumQos.integerValue
        buildPacket {
            writeMqttFilter(topicFilter)
            writeByte(qosInt)
        }
    }

    companion object {
        fun fromMany(buffer: ByteReadPacket): List<Subscription> {
            val subscriptions = ArrayList<Subscription>()
            while (buffer.remaining != 0.toLong()) {
                subscriptions.add(from(buffer))
            }
            return subscriptions
        }

        fun from(buffer: ByteReadPacket): Subscription {
            val topicFilter = buffer.readMqttFilter()
            val subOptionsInt = buffer.readUByte().toInt()
            val qosBit1 = subOptionsInt.shl(6).shr(7) == 1
            val qosBit0 = subOptionsInt.shl(7).shr(7) == 1
            val qos = QualityOfService.fromBooleans(qosBit1, qosBit0)
            return Subscription(topicFilter, qos)
        }

        fun from(topics: List<Filter>, qos: List<QualityOfService>): List<Subscription> {
            if (topics.size != qos.size) {
                throw IllegalArgumentException("Non matching topics collection size with the QoS collection size")
            }
            val subscriptions = mutableListOf<Subscription>()
            topics.forEachIndexed { index, topic ->
                subscriptions += Subscription(topic, qos[index])
            }
            return subscriptions
        }

        fun writeMany(subscriptions: Collection<Subscription>) = buildPacket {
            subscriptions.forEach {
                writePacket(it.packet)
            }
        }
    }
}
