@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire4.control.packet

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException
import mqtt.wire.control.packet.IPublishMessage
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.control.packet.getAndIncrementPacketIdentifier
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.*
import mqtt.wire.data.readMqttUtf8String
import mqtt.wire.data.topic.Name
import mqtt.wire.data.writeMqttName

/**
 * A PUBLISH Control Packet is sent from a Client to a Server or from Server to a Client to transport an
 * Application Message.
 */
data class PublishMessage(
        val fixed: FixedHeader = FixedHeader(),
        val variable: VariableHeader,
        val payload: ByteReadPacket? = null)
    : ControlPacketV4(3, DirectionOfFlow.BIDIRECTIONAL, fixed.flags), IPublishMessage {

    /**
     * Build a QOS 0 At most once publish message
     */
    constructor(topic: String, payload: ByteReadPacket? = null, dup: Boolean = false, retain: Boolean = false)
            : this(FixedHeader(dup, retain = retain), VariableHeader(Name(topic)), payload)

    constructor(topic: String, payload: String, dup: Boolean = false, retain: Boolean = false)
            : this(FixedHeader(dup, retain = retain), VariableHeader(Name(topic)),
            buildPacket { writeStringUtf8(payload) })

    /**
     * Build a QOS 1 or 2 publish message
     */
    constructor(topic: String, qos: QualityOfService, payload: ByteReadPacket? = null,
                packetIdentifier: UShort = getAndIncrementPacketIdentifier(), dup: Boolean = false, retain: Boolean = false)
            : this(FixedHeader(dup, qos, retain), VariableHeader(Name(topic), packetIdentifier), payload)

    constructor(topic: String, qos: QualityOfService,
                packetIdentifier: UShort = getAndIncrementPacketIdentifier(), dup: Boolean = false, retain: Boolean = false)
            : this(FixedHeader(dup, qos, retain), VariableHeader(Name(topic), packetIdentifier), null)

    init {
        if (fixed.qos == AT_MOST_ONCE && variable.packetIdentifier != null) {
            throw IllegalArgumentException("Cannot allocate a publish message with a QoS of 0 with a packet identifier")
        } else if (fixed.qos.isGreaterThan(AT_MOST_ONCE) && variable.packetIdentifier == null) {
            throw IllegalArgumentException("Cannot allocate a publish message with a QoS >0 and no packet identifier")
        }
    }

    override val qualityOfService: QualityOfService = fixed.qos
    override val variableHeaderPacket: ByteReadPacket = variable.packet()
    override fun payloadPacket(sendDefaults: Boolean) = payload

    override fun expectedResponse() = when {
        fixed.qos == AT_LEAST_ONCE -> {
            PublishAcknowledgment(variable.packetIdentifier!!)
        }
        fixed.qos == EXACTLY_ONCE -> {
            PublishRelease(variable.packetIdentifier!!)
        }
        else -> null
    }

    override val topic: Name = variable.topicName

    data class FixedHeader(
            /**
             * 3.3.1.1 DUP
             *
             * Position: byte 1, bit 3.
             *
             * If the DUP flag is set to 0, it indicates that this is the first occasion that the Client or Server
             * has attempted to send this MQTT PUBLISH Packet. If the DUP flag is set to 1, it indicates that this
             * might be re-delivery of an earlier attempt to send the Packet.
             *
             * The DUP flag MUST be set to 1 by the Client or Server when it attempts to re-deliver a PUBLISH Packet
             * [MQTT-3.3.1.-1]. The DUP flag MUST be set to 0 for all QoS 0 messages [MQTT-3.3.1-2].
             *
             * The value of the DUP flag from an incoming PUBLISH packet is not propagated when the PUBLISH Packet is
             * sent to subscribers by the Server. The DUP flag in the outgoing PUBLISH packet is set independently to
             * the incoming PUBLISH packet, its value MUST be determined solely by whether the outgoing PUBLISH packet
             * is a retransmission [MQTT-3.3.1-3].
             *
             * Non normative comment
             *
             * The recipient of a Control Packet that contains the DUP flag set to 1 cannot assume that it has seen an
             * earlier copy of this packet.
             *
             * Non normative comment
             *
             * It is important to note that the DUP flag refers to the Control Packet itself and not to the
             * Application Message that it contains. When using QoS 1, it is possible for a Client to receive
             * a PUBLISH Packet with DUP flag set to 0 that contains a repetition of an Application Message
             * that it received earlier, but with a different Packet Identifier. Section 2.3.1 provides more
             * information about Packet Identifiers.
             */
            val dup: Boolean = false,
            /**
             * 3.3.1.2 QoS
             *
             * Position: byte 1, bits 2-1.
             *
             * This field indicates the level of assurance for delivery of an Application Message. The QoS levels are
             * listed in the Table 3.2 - QoS definitions, below.
             *
             * A PUBLISH Packet MUST NOT have both QoS bits set to 1. If a Server or Client receives a PUBLISH Packet
             * which has both QoS bits set to 1 it MUST close the Network Connection [MQTT-3.3.1-4].
             */
            val qos: QualityOfService = AT_MOST_ONCE,
            /**
             * 3.3.1.3 RETAIN
             *
             * Position: byte 1, bit 0.
             *
             * If the RETAIN flag is set to 1, in a PUBLISH Packet sent by a Client to a Server, the Server MUST store
             * the Application Message and its QoS, so that it can be delivered to future subscribers whose
             * subscriptions match its topic name [MQTT-3.3.1-5]. When a new subscription is established, the last
             * retained message, if any, on each matching topic name MUST be sent to the subscriber [MQTT-3.3.1-6].
             * If the Server receives a QoS 0 message with the RETAIN flag set to 1 it MUST discard any message
             * previously retained for that topic. It SHOULD store the new QoS 0 message as the new retained message
             * for that topic, but MAY choose to discard it at any time - if this happens there will be no retained
             * message for that topic [MQTT-3.3.1-7]. See Section 4.1 for more information on storing state.
             *
             * When sending a PUBLISH Packet to a Client the Server MUST set the RETAIN flag to 1 if a message is sent
             * as a result of a new subscription being made by a Client [MQTT-3.3.1-8]. It MUST set the RETAIN flag to
             * 0 when a PUBLISH Packet is sent to a Client because it matches an established subscription regardless
             * of how the flag was set in the message it received [MQTT-3.3.1-9].
             *
             * A PUBLISH Packet with a RETAIN flag set to 1 and a payload containing zero bytes will be processed as
             * normal by the Server and sent to Clients with a subscription matching the topic name. Additionally
             * any existing retained message with the same topic name MUST be removed and any future subscribers
             * for the topic will not receive a retained message [MQTT-3.3.1-10]. “As normal” means that the RETAIN
             * flag is not set in the message received by existing Clients. A zero byte retained message MUST NOT
             * be stored as a retained message on the Server [MQTT-3.3.1-11].
             *
             * If the RETAIN flag is 0, in a PUBLISH Packet sent by a Client to a Server, the Server MUST NOT store
             * the message and MUST NOT remove or replace any existing retained message [MQTT-3.3.1-12].
             *
             * Non normative comment
             *
             * Retained messages are useful where publishers send state messages on an irregular basis. A new
             * subscriber will receive the most recent state.
             */
            val retain: Boolean = false) {
        val flags by lazy {
            val dupInt = if (dup) 0b1000 else 0b0
            val qosInt = qos.integerValue.toInt().shl(1)
            val retainInt = if (retain) 0b1 else 0b0
            (dupInt or qosInt or retainInt).toByte()
        }

        companion object {
            fun fromByte(byte1: UByte): FixedHeader {
                val byte1Int = byte1.toInt()
                val dup = byte1Int.shl(4).toUByte().toInt().shr(7) == 1
                val qosBit2 = byte1Int.shl(5).toUByte().toInt().shr(7) == 1
                val qosBit1 = byte1Int.shl(6).toUByte().toInt().shr(7) == 1
                if (qosBit2 && qosBit1) {
                    throw MalformedPacketException("A PUBLISH Packet MUST NOT have both QoS bits set to 1 [MQTT-3.3.1-4]." +
                            " If a Server or Client receives a PUBLISH packet which has both QoS bits set to 1 it is a " +
                            "Malformed Packet. Use DISCONNECT with Reason Code 0x81 (Malformed Packet) as described in" +
                            " section 4.13.")
                }
                val qos = QualityOfService.fromBooleans(qosBit2, qosBit1)
                val retain = byte1Int.shl(7).toUByte().toInt().shr(7) == 1
                return FixedHeader(dup, qos, retain)
            }
        }
    }

    /**
     * 3.3.2 PUBLISH Variable Header
     *
     * The variable header contains the following fields in the order: Topic Name, Packet Identifier.
     */
    data class VariableHeader(
            /**
             * The Topic Name identifies the information channel to which payload data is published.
             *
             * The Topic Name MUST be present as the first field in the PUBLISH Packet Variable header. It MUST be a
             * UTF-8 encoded string [MQTT-3.3.2-1] as defined in section 1.5.3.
             *
             * The Topic Name in the PUBLISH Packet MUST NOT contain wildcard characters [MQTT-3.3.2-2].
             *
             * The Topic Name in a PUBLISH Packet sent by a Server to a subscribing Client MUST match the
             * Subscription’s Topic Filter according to the matching process defined in Section 4.7
             * [MQTT-3.3.2-3]. However, since the Server is permitted to override the Topic Name, it might not be the
             * same as the Topic Name in the original PUBLISH Packet.
             */
            val topicName: Name,
            /**
             * The Packet Identifier field is only present in PUBLISH Packets where the QoS level is 1 or 2. Section
             * 2.3.1 provides more information about Packet Identifiers.
             */
            val packetIdentifier: UShort? = null) {

        fun packet() = buildPacket {
            writeMqttName(topicName)
            if (packetIdentifier != null) {
                writeUShort(packetIdentifier)
            }
        }

        companion object {
            fun from(buffer: ByteReadPacket, isQos0: Boolean): VariableHeader {
                val topicName = buffer.readMqttUtf8String()
                val packetIdentifier = if (isQos0) null else buffer.readUShort()
                return VariableHeader(Name(topicName.getValueOrThrow()), packetIdentifier)
            }
        }
    }

    companion object {
        fun from(buffer: ByteReadPacket, byte1: UByte): PublishMessage {
            val fixedHeader = FixedHeader.fromByte(byte1)
            val variableHeader = VariableHeader.from(buffer, fixedHeader.qos == AT_MOST_ONCE)
            val payloadBytes = buildPacket {
                writeFully(buffer.readBytes())
            }
            return PublishMessage(fixedHeader, variableHeader, payloadBytes)
        }
    }

}
