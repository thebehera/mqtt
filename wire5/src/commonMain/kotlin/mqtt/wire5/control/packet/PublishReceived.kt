@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import mqtt.IgnoredOnParcel
import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.IPublishReceived
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire5.control.packet.format.variable.property.Property
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties

/**
 * 3.5 PUBREC – Publish received (QoS 2 delivery part 1)
 *
 * A PUBREC packet is the response to a PUBLISH packet with QoS 2. It is the second packet of the QoS 2 protocol exchange.
 */
@Parcelize
data class PublishReceived(val variable: VariableHeader)
    : ControlPacketV5(5, DirectionOfFlow.BIDIRECTIONAL), IPublishReceived {
    override fun expectedResponse() = PublishRelease(variable.packetIdentifier.toUShort())
    @IgnoredOnParcel override val packetIdentifier: Int = variable.packetIdentifier
    override fun variableHeader(writeBuffer: WriteBuffer) = variable.serialize(writeBuffer)
    override fun remainingLength(buffer: WriteBuffer) = variable.size(buffer)

    @Parcelize
    data class VariableHeader(
        val packetIdentifier: Int,
        /**
         * 3.5.2.1 PUBREC Reason Code
         * Byte 3 in the Variable Header is the PUBREC Reason Code. If the Remaining Length is
         * 2, then the Publish Reason Code has the value 0x00 (Success).
         * The Client or Server sending the PUBREC packet MUST use one of the PUBREC Reason Code
         * values. [MQTT-3.5.2-1]. The Reason Code and Property Length can be omitted if the
         * Reason Code is 0x00 (Success) and there are no Properties. In this case the PUBREC
         * has a Remaining Length of 2.
         */
        val reasonCode: ReasonCode = SUCCESS,
        /**
         * 3.4.2.2 PUBACK Properties
         */
        val properties: Properties = Properties()
    ) : Parcelable {
        init {
            when (reasonCode.byte.toInt()) {
                0, 0x10, 0x80, 0x83, 0x87, 0x90, 0x91, 0x97, 0x99 -> {
                }
                else -> throw ProtocolError(
                    "Invalid Publish Receieved reason code ${reasonCode.byte} " +
                            "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477424"
                )
            }
        }

        fun size(buffer: WriteBuffer): UInt {
            val canOmitReasonCodeAndProperties = (reasonCode == SUCCESS
                    && properties.userProperty.isEmpty()
                    && properties.reasonString == null)
            var size = UShort.SIZE_BYTES.toUInt()
            if (!canOmitReasonCodeAndProperties) {
                val propsSize = properties.size(buffer)
                size += UByte.SIZE_BYTES.toUInt() + buffer.variableByteIntegerSize(propsSize) + propsSize
            }
            return size
        }

        fun serialize(writeBuffer: WriteBuffer) {
            writeBuffer.write(packetIdentifier.toUShort())
            val canOmitReasonCodeAndProperties = (reasonCode == SUCCESS
                    && properties.userProperty.isEmpty()
                    && properties.reasonString == null)
            if (!canOmitReasonCodeAndProperties) {
                writeBuffer.write(reasonCode.byte)
                properties.serialize(writeBuffer)
            }
        }

        @Parcelize
        data class Properties(
            /**
             * 3.5.2.2.2 Reason String
             *
             * 31 (0x1F) Byte, Identifier of the Reason String.
             *
             * Followed by the UTF-8 Encoded String representing the reason associated with this response. This
             * Reason String is human readable, designed for diagnostics and SHOULD NOT be parsed by the receiver.
             *
             * The sender uses this value to give additional information to the receiver. The sender MUST NOT
             * send this property if it would increase the size of the PUBREC packet beyond the Maximum Packet
             * Size specified by the receiver [MQTT-3.5.2-2]. It is a Protocol Error to include the Reason
             * String more than once.
             */
            val reasonString: CharSequence? = null,
            /**
             * 3.5.2.2.3 User Property
             *
             * 38 (0x26) Byte, Identifier of the User Property.
             *
             * Followed by UTF-8 String Pair. This property can be used to provide additional diagnostic or other
             * information. The sender MUST NOT send this property if it would increase the size of the PUBREC
             * packet beyond the Maximum Packet Size specified by the receiver [MQTT-3.5.2-3]. The User Property
             * is allowed to appear multiple times to represent multiple name, value pairs. The same name is
             * allowed to appear more than once.
             */
            val userProperty: List<Pair<CharSequence, CharSequence>> = emptyList()
        ) : Parcelable {
            @IgnoredOnParcel
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

            fun size(buffer: WriteBuffer): UInt {
                var size = 0u
                props.forEach { size += it.size(buffer) }
                return size
            }

            fun serialize(buffer: WriteBuffer) {
                buffer.writeVariableByteInteger(size(buffer))
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
                                                "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477427"
                                    )
                                }
                                reasonString = it.diagnosticInfoDontParse
                            }
                            is UserProperty -> userProperty += Pair(it.key, it.value)
                            else -> throw MalformedPacketException("Invalid Publish Received property type found in MQTT properties $it")
                        }
                    }
                    return Properties(reasonString, userProperty)
                }
            }
        }

        companion object {

            fun from(buffer: ReadBuffer, remainingLength: UInt): VariableHeader {
                val packetIdentifier = buffer.readUnsignedShort().toInt()
                if (remainingLength == 2u) {
                    return VariableHeader(packetIdentifier)
                } else {
                    val reasonCodeByte = buffer.readUnsignedByte()
                    val reasonCode = when (reasonCodeByte) {
                        SUCCESS.byte -> SUCCESS
                        NO_MATCHING_SUBSCRIBERS.byte -> NO_MATCHING_SUBSCRIBERS
                        UNSPECIFIED_ERROR.byte -> UNSPECIFIED_ERROR
                        IMPLEMENTATION_SPECIFIC_ERROR.byte -> IMPLEMENTATION_SPECIFIC_ERROR
                        NOT_AUTHORIZED.byte -> NOT_AUTHORIZED
                        TOPIC_NAME_INVALID.byte -> TOPIC_NAME_INVALID
                        PACKET_IDENTIFIER_IN_USE.byte -> PACKET_IDENTIFIER_IN_USE
                        QUOTA_EXCEEDED.byte -> QUOTA_EXCEEDED
                        PAYLOAD_FORMAT_INVALID.byte -> PAYLOAD_FORMAT_INVALID
                        else -> throw MalformedPacketException(
                            "Invalid reason code $reasonCodeByte" +
                                    "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477424"
                        )
                    }
                    val propsData = buffer.readProperties()
                    val props = Properties.from(propsData)
                    return VariableHeader(packetIdentifier, reasonCode, props)
                }
            }
        }
    }

    companion object {
        fun from(buffer: ReadBuffer, remainingLength: UInt) =
            PublishReceived(VariableHeader.from(buffer, remainingLength))
    }
}
