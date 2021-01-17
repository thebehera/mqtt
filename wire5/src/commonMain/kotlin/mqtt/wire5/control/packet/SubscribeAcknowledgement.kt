@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.buffer.variableByteIntegerSize
import mqtt.wire.buffer.writeVariableByteInteger
import mqtt.wire.control.packet.ISubscribeAcknowledgement
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire5.control.packet.SubscribeAcknowledgement.VariableHeader.Properties
import mqtt.wire5.control.packet.format.variable.property.Property
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readPropertiesSized

/**
 * 3.9 SUBACK – Subscribe acknowledgement
 *
 * A SUBACK packet is sent by the Server to the Client to confirm receipt and processing of a SUBSCRIBE packet.
 *
 * A SUBACK packet contains a list of Reason Codes, that specify the maximum QoS level that was granted or the
 * error which was found for each Subscription that was requested by the SUBSCRIBE.
 */
data class SubscribeAcknowledgement(val variable: VariableHeader, val payload: List<ReasonCode>) :
    ControlPacketV5(9, DirectionOfFlow.SERVER_TO_CLIENT), ISubscribeAcknowledgement {
    constructor(packetIdentifier: UShort, properties: Properties = Properties(), payload: ReasonCode = SUCCESS)
            : this(VariableHeader(packetIdentifier.toInt(), properties), listOf(payload))

    constructor(packetIdentifier: UShort, payload: ReasonCode = SUCCESS, properties: Properties = Properties())
            : this(VariableHeader(packetIdentifier.toInt(), properties), listOf(payload))

    override val packetIdentifier: Int = variable.packetIdentifier.toInt()
    override fun variableHeader(writeBuffer: WriteBuffer) = variable.serialize(writeBuffer)
    override fun payload(writeBuffer: WriteBuffer) = payload.forEach { writeBuffer.write(it.byte) }
    override fun remainingLength() = variable.size() + payload.size.toUInt()

    init {
        payload.forEach {
            if (!validSubscribeCodes.contains(it)) {
                throw ProtocolError("Invalid SUBACK reason code ${it.byte}")
            }
        }
    }

    /**
     * 3.9.2 SUBACK Variable Header
     *
     * The Variable Header of the SUBACK Packet contains the following fields in the order: the Packet Identifier from
     * the SUBSCRIBE Packet that is being acknowledged, and Properties.
     */
    data class VariableHeader(
        val packetIdentifier: Int,
        val properties: Properties = Properties()
    ) {
        fun serialize(writeBuffer: WriteBuffer) {
            writeBuffer.write(packetIdentifier.toUShort())
            properties.serialize(writeBuffer)
        }

        fun size(): UInt {
            var size = UShort.SIZE_BYTES.toUInt()
            val propsSize = properties.size()
            size += variableByteIntegerSize(propsSize) + propsSize
            return size
        }

        /**
         * 3.9.2.1 SUBACK Properties
         */
        data class Properties(
            /**
             * 3.9.2.1.2 Reason String
             *
             * 31 (0x1F) Byte, Identifier of the Reason String.
             *
             * Followed by the UTF-8 Encoded String representing the reason associated with this response. This
             * Reason String is a human readable string designed for diagnostics and SHOULD NOT be parsed by the Client.
             *
             * The Server uses this value to give additional information to the Client. The Server MUST NOT send this
             * Property if it would increase the size of the SUBACK packet beyond the Maximum Packet Size specified by
             * the Client [MQTT-3.9.2-1]. It is a Protocol Error to include the Reason String more than once.
             */
            val reasonString: CharSequence? = null,
            /**
             * 3.4.2.2.3 User Property
             *
             * 38 (0x26) Byte, Identifier of the User Property.
             *
             * Followed by UTF-8 String Pair. This property can be used to provide additional diagnostic or
             * other information. The sender MUST NOT send this property if it would increase the size of the
             * PUBACK packet beyond the Maximum Packet Size specified by the receiver [MQTT-3.4.2-3]. The User
             * Property is allowed to appear multiple times to represent multiple name, value pairs. The same
             * name is allowed to appear more than once.
             */
            val userProperty: List<Pair<CharSequence, CharSequence>> = emptyList()
        ) {
            val props by lazy {
                val props = ArrayList<Property>(1 + userProperty.size)
                if (reasonString != null) {
                    props += ReasonString(reasonString)
                }
                if (userProperty.isNotEmpty()) {
                    for (keyValueProperty in userProperty) {
                        val key = keyValueProperty.first
                        val value = keyValueProperty.second
                        props += UserProperty(key, value)
                    }
                }
                props
            }

            fun size(): UInt {
                var size = 0u
                props.forEach { size += it.size() }
                return size
            }

            fun serialize(buffer: WriteBuffer) {
                buffer.writeVariableByteInteger(size())
                props.forEach { it.write(buffer) }
            }

            companion object {
                fun from(keyValuePairs: Collection<Property>?): Properties {
                    var reasonString: CharSequence? = null
                    val userProperty = mutableListOf<Pair<CharSequence, CharSequence>>()
                    keyValuePairs?.forEach {
                        when (it) {
                            is ReasonString -> {
                                if (reasonString != null) {
                                    throw ProtocolError(
                                        "Reason String added multiple times see: " +
                                                "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477476"
                                    )
                                }
                                reasonString = it.diagnosticInfoDontParse
                            }
                            is UserProperty -> userProperty += Pair(it.key, it.value)
                            else -> throw MalformedPacketException("Invalid Subscribe ack property type found in MQTT properties $it")
                        }
                    }
                    return Properties(reasonString, userProperty)
                }
            }
        }

        companion object {
            fun from(buffer: ReadBuffer, remainingLength: UInt): Pair<UInt, VariableHeader> {
                val packetIdentifier = buffer.readUnsignedShort()
                var bytesRead = 3u
                val props = if (remainingLength > bytesRead) {
                    val sized = buffer.readPropertiesSized()
                    bytesRead += sized.first
                    Properties.from(sized.second)
                } else {
                    Properties()
                }
                return Pair(bytesRead, VariableHeader(packetIdentifier.toInt(), props))
            }
        }
    }

    companion object {
        fun from(buffer: ReadBuffer, remainingLength: UInt): SubscribeAcknowledgement {
            val variableHeader = VariableHeader.from(buffer, remainingLength)
            val max = (remainingLength - variableHeader.first).toInt()
            val codes = ArrayList<ReasonCode>(max)
            while (codes.size < max) {
                val reasonCode = when (val reasonCodeByte = buffer.readUnsignedByte()) {
                    GRANTED_QOS_0.byte -> GRANTED_QOS_0
                    GRANTED_QOS_1.byte -> GRANTED_QOS_1
                    GRANTED_QOS_2.byte -> GRANTED_QOS_2
                    UNSPECIFIED_ERROR.byte -> UNSPECIFIED_ERROR
                    IMPLEMENTATION_SPECIFIC_ERROR.byte -> IMPLEMENTATION_SPECIFIC_ERROR
                    NOT_AUTHORIZED.byte -> NOT_AUTHORIZED
                    TOPIC_FILTER_INVALID.byte -> TOPIC_FILTER_INVALID
                    PACKET_IDENTIFIER_IN_USE.byte -> PACKET_IDENTIFIER_IN_USE
                    QUOTA_EXCEEDED.byte -> QUOTA_EXCEEDED
                    SHARED_SUBSCRIPTIONS_NOT_SUPPORTED.byte -> SHARED_SUBSCRIPTIONS_NOT_SUPPORTED
                    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED.byte -> SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED
                    WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED.byte -> WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED
                    else -> throw MalformedPacketException(
                        "Invalid reason code $reasonCodeByte " +
                                "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477478"
                    )
                }
                codes += reasonCode
            }
            return SubscribeAcknowledgement(variableHeader.second, codes)
        }
    }
}

private val validSubscribeCodes by lazy {
    setOf(
        GRANTED_QOS_0,
        GRANTED_QOS_1,
        GRANTED_QOS_2,
        UNSPECIFIED_ERROR,
        IMPLEMENTATION_SPECIFIC_ERROR,
        NOT_AUTHORIZED,
        TOPIC_FILTER_INVALID,
        PACKET_IDENTIFIER_IN_USE,
        QUOTA_EXCEEDED,
        SHARED_SUBSCRIPTIONS_NOT_SUPPORTED,
        SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED,
        WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED
    )
}
