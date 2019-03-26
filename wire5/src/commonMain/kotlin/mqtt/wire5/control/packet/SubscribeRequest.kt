@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.*
import mqtt.wire5.control.packet.RetainHandling.*
import mqtt.wire5.control.packet.format.variable.property.Property
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties

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
data class SubscribeRequest(val variable: VariableHeader, val subscriptions: Collection<Subscription>)
    : ControlPacketV5(8, DirectionOfFlow.CLIENT_TO_SERVER, 0b10) {

    override val variableHeaderPacket = variable.packet()
    override fun payloadPacket(sendDefaults: Boolean) = buildPacket {
        subscriptions.forEach { writePacket(it.packet) }
    }

    /**
     * 3.8.2 SUBSCRIBE Variable Header
     *
     * The Variable Header of the SUBSCRIBE Packet contains the following fields in the order: Packet Identifier, and
     * Properties. Section 2.2.1 provides more information about Packet Identifiers. The rules for encoding Properties
     * are described in section 2.2.2.
     *
     * Non-normative example
     *
     * Figure 3-19 shows an example of a SUBSCRIBE variable header with a Packet Identifier of 10 and no properties.
     */
    data class VariableHeader(val packetIdentifier: UShort,
                              val properties: Properties = Properties()) {
        fun packet(): ByteReadPacket {
            return buildPacket {
                writeUShort(packetIdentifier)
                writePacket(properties.packet())
            }
        }

        data class Properties(
                /**
                 * 3.8.2.1.2 Subscription Identifier
                 *
                 * 11 (0x0B) Byte, Identifier of the Subscription Identifier.
                 *
                 * Followed by a Variable Byte Integer representing the identifier of the subscription. The
                 * Subscription Identifier can have the value of 1 to 268,435,455. It is a Protocol Error if the
                 * Subscription Identifier has a value of 0. It is a Protocol Error to include the Subscription
                 * Identifier more than once.
                 *
                 * The Subscription Identifier is associated with any subscription created or modified as the result
                 * of this SUBSCRIBE packet. If there is a Subscription Identifier, it is stored with the
                 * subscription. If this property is not specified, then the absence of a Subscription Identifier
                 * is stored with the subscription.
                 *
                 * Refer to section 3.8.3.1 for more information about the handling of Subscription Identifiers.
                 */
                val reasonString: MqttUtf8String? = null,
                /**
                 * 3.8.2.1.3 User Property
                 *
                 * 38 (0x26) Byte, Identifier of the User Property.
                 *
                 * Followed by a UTF-8 String Pair.
                 *
                 * The User Property is allowed to appear multiple times to represent multiple name, value pairs. The
                 * same name is allowed to appear more than once.
                 *
                 * Non-normative comment
                 *
                 * User Properties on the SUBSCRIBE packet can be used to send subscription related properties from
                 * the Client to the Server. The meaning of these properties is not defined by this specification.
                 */
                val userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = emptyList()) {
            fun packet(): ByteReadPacket {
                val propertiesPacket = buildPacket {
                    if (reasonString != null) {
                        ReasonString(reasonString).write(this)
                    }
                    if (userProperty.isNotEmpty()) {
                        for (keyValueProperty in userProperty) {
                            val key = keyValueProperty.first
                            val value = keyValueProperty.second
                            UserProperty(key, value).write(this)
                        }
                    }
                }
                val propertyLength = propertiesPacket.remaining
                return buildPacket {
                    writePacket(VariableByteInteger(propertyLength.toUInt()).encodedValue())
                    writePacket(propertiesPacket)
                }
            }

            companion object {
                fun from(keyValuePairs: Collection<Property>?): Properties {
                    var reasonString: MqttUtf8String? = null
                    var userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = mutableListOf()
                    keyValuePairs?.forEach {
                        when (it) {
                            is ReasonString -> {
                                if (reasonString != null) {
                                    throw ProtocolError("Reason String added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477427")
                                }
                                reasonString = it.diagnosticInfoDontParse
                            }
                            is UserProperty -> userProperty += Pair(it.key, it.value)
                            else -> throw MalformedPacketException("Invalid Subscribe Request property type found in MQTT properties $it")
                        }
                    }
                    return Properties(reasonString, userProperty)
                }
            }
        }

        companion object {
            fun from(buffer: ByteReadPacket): VariableHeader {
                val packetIdentifier = buffer.readUShort()
                val remaining = buffer.remaining.toInt()
                return if (remaining == 2) {
                    VariableHeader(packetIdentifier)
                } else {
                    val propsData = buffer.readProperties()
                    val props = VariableHeader.Properties.from(propsData)
                    VariableHeader(packetIdentifier, props)
                }
            }
        }
    }

    companion object {
        fun from(buffer: ByteReadPacket): SubscribeRequest {
            val header = VariableHeader.from(buffer)
            val subscriptions = Subscription.fromMany(buffer)
            return SubscribeRequest(header, subscriptions)
        }
    }
}

data class Subscription(val topicFilter: MqttUtf8String,
                        /**
                         * Bits 0 and 1 of the Subscription Options represent Maximum QoS field. This gives the maximum
                         * QoS level at which the Server can send Application Messages to the Client. It is a Protocol
                         * Error if the Maximum QoS field has the value 3.
                         */
                        val maximumQos: QualityOfService = QualityOfService.AT_LEAST_ONCE,
                        /**
                         * Bit 2 of the Subscription Options represents the No Local option. If the value is 1,
                         * Application Messages MUST NOT be forwarded to a connection with a ClientID equal to the
                         * ClientID of the publishing connection [MQTT-3.8.3-3]. It is a Protocol Error to set the No
                         * Local bit to 1 on a Shared Subscription [MQTT-3.8.3-4].
                         */
                        val noLocal: Boolean = false,
                        /**
                         * Bit 3 of the Subscription Options represents the Retain As Published option. If 1,
                         * Application Messages forwarded using this subscription keep the RETAIN flag they were
                         * published with. If 0, Application Messages forwarded using this subscription have the
                         * RETAIN flag set to 0. Retained messages sent when the subscription is established have
                         * the RETAIN flag set to 1.
                         */
                        val retainAsPublished: Boolean = false,
                        /**
                         * Bits 4 and 5 of the Subscription Options represent the Retain Handling option. This option
                         * specifies whether retained messages are sent when the subscription is established. This
                         * does not affect the sending of retained messages at any point after the subscribe. If there
                         * are no retained messages matching the Topic Filter, all of these values act the same. The
                         * values are:
                         *
                         * 0 = Send retained messages at the time of the subscribe
                         *
                         * 1 = Send retained messages at subscribe only if the subscription does not currently exist
                         *
                         * 2 = Do not send retained messages at the time of the subscribe
                         *
                         * It is a Protocol Error to send a Retain Handling value of 3.
                         */
                        val retainHandling: RetainHandling = SEND_RETAINED_MESSAGES_AT_SUBSCRIBE_ONLY_IF_SUBSCRIBE_DOESNT_EXISTS) {
    val packet by lazy {
        val qosInt = maximumQos.integerValue
        val nlShifted = (if (noLocal) 1 else 0).shl(2)
        val rapShifted = (if (retainAsPublished) 1 else 0).shl(3)
        val rH = retainHandling.value.toInt().shl(4)
        val combinedByte = (qosInt + nlShifted + rapShifted + rH).toByte()
        buildPacket {
            writeMqttUtf8String(topicFilter)
            writeByte(combinedByte)
            writeByte(qosInt)
        }
    }

    companion object {
        fun fromMany(buffer: ByteReadPacket): Collection<Subscription> {
            val subscriptions = HashSet<Subscription>()
            while (buffer.remaining > 1.toLong()) {
                subscriptions.add(from(buffer))
            }
            return subscriptions
        }

        fun from(buffer: ByteReadPacket): Subscription {
            val topicFilter = buffer.readMqttUtf8String()
            val subOptionsInt = buffer.readUByte().toInt()
            val reservedBit7 = subOptionsInt.shr(7) == 1
            if (reservedBit7) {
                throw ProtocolError("Bit 7 in Subscribe payload is set to an invalid value (it is reserved)")
            }
            val reservedBit6 = subOptionsInt.shl(1).shr(7) == 1
            if (reservedBit6) {
                throw ProtocolError("Bit 7 in Subscribe payload is set to an invalid value (it is reserved)")
            }
            val retainHandlingBit5 = subOptionsInt.shl(2).shr(7) == 1
            val retainHandlingBit4 = subOptionsInt.shl(3).shr(7) == 1
            val retainHandling = if (retainHandlingBit5 && retainHandlingBit4) {
                throw ProtocolError("Retain Handling Value cannot be set to 3")
            } else if (retainHandlingBit5 && !retainHandlingBit4) {
                DO_NOT_SEND_RETAINED_MESSAGES
            } else if (!retainHandlingBit5 && retainHandlingBit4) {
                SEND_RETAINED_MESSAGES_AT_SUBSCRIBE_ONLY_IF_SUBSCRIBE_DOESNT_EXISTS
            } else {
                SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE
            }
            val rapBit3 = subOptionsInt.shl(4).shr(7) == 1
            val nlBit2 = subOptionsInt.shl(5).shr(7) == 1
            val qosBit1 = subOptionsInt.shl(6).shr(7) == 1
            val qosBit0 = subOptionsInt.shl(7).shr(7) == 1
            val qos = QualityOfService.fromBooleans(qosBit1, qosBit0)
            return Subscription(topicFilter, qos, nlBit2, rapBit3, retainHandling)
        }
    }
}

enum class RetainHandling(val value: UByte) {
    SEND_RETAINED_MESSAGES_AT_TIME_OF_SUBSCRIBE(0.toUByte()),
    SEND_RETAINED_MESSAGES_AT_SUBSCRIBE_ONLY_IF_SUBSCRIBE_DOESNT_EXISTS(1.toUByte()),
    DO_NOT_SEND_RETAINED_MESSAGES(2.toUByte())
}
