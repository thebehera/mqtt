@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.*
import mqtt.IgnoredOnParcel
import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.control.packet.ISubscribeRequest
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
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
data class
SubscribeRequest(
    override val packetIdentifier: Int,
                            val subscriptions: List<Subscription>) :
    ControlPacketV4(ISubscribeRequest.controlPacketValue, DirectionOfFlow.CLIENT_TO_SERVER, 0b10), ISubscribeRequest {

    constructor(packetIdentifier: UShort, topic: Filter, qos: QualityOfService)
            : this(packetIdentifier.toInt(), subscriptions = listOf(Subscription(topic, qos)))


    constructor(packetIdentifier: UShort, topics: List<Filter>, qos: List<QualityOfService>)
            : this(packetIdentifier.toInt(), subscriptions = Subscription.from(topics, qos))

    @IgnoredOnParcel
    private val payloadSubs by lazy { Subscription.writeMany(subscriptions) }

    @IgnoredOnParcel
    override val variableHeaderPacket = buildPacket {
        writeUShort(packetIdentifier.toUShort())
    }

    override fun variableHeader(writeBuffer: WriteBuffer) {
        writeBuffer.write(packetIdentifier.toUShort())
    }

    override fun payloadPacket(sendDefaults: Boolean) = payloadSubs
    override fun payload(writeBuffer: WriteBuffer) = Subscription.writeMany(subscriptions, writeBuffer)
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
            val packetIdentifier = buffer.readUShort().toInt()
            val subscriptions = Subscription.fromMany(buffer)
            return SubscribeRequest(packetIdentifier, subscriptions)
        }

        fun from(buffer: ReadBuffer, remaining: UInt): SubscribeRequest {
            val packetIdentifier = buffer.readUnsignedShort().toInt()
            val subscriptions = Subscription.fromMany(buffer, remaining - UShort.SIZE_BYTES.toUInt())
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
    @IgnoredOnParcel
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

        fun fromMany(buffer: ReadBuffer, remaining: UInt): List<Subscription> {
            val subscriptions = ArrayList<Subscription>()
            var bytesRead = 0
            while (bytesRead.toUInt() < remaining) {
                val result = from(buffer)
                bytesRead += result.first.toInt()
                subscriptions.add(result.second)
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

        fun from(buffer: ReadBuffer): Pair<UInt, Subscription> {
            val result = buffer.readMqttUtf8StringNotValidatedSized()
            var bytesRead = UShort.SIZE_BYTES.toUInt() + result.first
            val topicFilter = result.second
            val subOptionsInt = buffer.readUnsignedByte().toInt()
            bytesRead++
            val qosBit1 = subOptionsInt.shl(6).shr(7) == 1
            val qosBit0 = subOptionsInt.shl(7).shr(7) == 1
            val qos = QualityOfService.fromBooleans(qosBit1, qosBit0)
            return Pair(bytesRead, Subscription(Filter(topicFilter), qos))
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

        fun writeMany(subscriptions: Collection<Subscription>, writeBuffer: WriteBuffer) {
            subscriptions.forEach {
                writeBuffer.writeUtf8String(it.topicFilter.topicFilter)
                writeBuffer.write(it.maximumQos.integerValue)
            }
        }
    }
}
