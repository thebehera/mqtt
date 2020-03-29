@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.*
import mqtt.IgnoredOnParcel
import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VariableByteInteger
import mqtt.wire5.control.packet.format.variable.property.*

@Parcelize
data class UnsubscribeAcknowledgment(val variable: VariableHeader, val reasonCodes: List<ReasonCode> = listOf(SUCCESS)) : ControlPacketV5(11, DirectionOfFlow.SERVER_TO_CLIENT) {
    @IgnoredOnParcel
    override val variableHeaderPacket: ByteReadPacket = variable.packet
    override fun payloadPacket(sendDefaults: Boolean) = buildPacket { reasonCodes.forEach { writeUByte(it.byte) } }
    override fun variableHeader(writeBuffer: WriteBuffer) = variable.serialize(writeBuffer)
    override fun remainingLength(buffer: WriteBuffer): UInt {
        val variableSize = variable.size(buffer)
        val subSize = reasonCodes.size.toUInt()
        return variableSize + subSize
    }

    override fun payload(writeBuffer: WriteBuffer) = reasonCodes.forEach { writeBuffer.write(it.byte) }

    init {
        val invalidCodes = reasonCodes.map { it.byte } - validSubscribeCodes
        if (invalidCodes.isEmpty()) {
            throw ProtocolError("Invalid SUBACK reason code $invalidCodes")
        }
    }

    /**
     * 3.11.2 UNSUBACK Variable Header
     *
     * The Variable Header of the UNSUBACK Packet the following fields in the order: the Packet Identifier from the
     * UNSUBSCRIBE Packet that is being acknowledged, and Properties. The rules for encoding Properties are described
     * in section 2.2.2.
     */
    @Parcelize
    data class VariableHeader(
        val packetIdentifier: Int,
        val properties: Properties = Properties()
    ) : Parcelable {
        @IgnoredOnParcel
        val packet by lazy {
            buildPacket {
                writeUShort(packetIdentifier.toUShort())
                writePacket(properties.packet)
            }
        }

        fun size(writeBuffer: WriteBuffer) =
            UShort.SIZE_BYTES.toUInt() + writeBuffer.variableByteIntegerSize(properties.size(writeBuffer)) + properties.size(
                writeBuffer
            )

        fun serialize(writeBuffer: WriteBuffer) {
            writeBuffer.write(packetIdentifier.toUShort())
            properties.serialize(writeBuffer)
        }

        /**
         * 3.9.2.1 SUBACK Properties
         */
        @Parcelize
        data class Properties(
            /**
             * 3.11.2.1.2 Reason String
             *
             * 31 (0x1F) Byte, Identifier of the Reason String.
             *
             * Followed by the UTF-8 Encoded String representing the reason associated with this response. This
             * Reason String is a human readable string designed for diagnostics and SHOULD NOT be parsed by the
             * Client.
             *
             * The Server uses this value to give additional information to the Client. The Server MUST NOT send
             * this Property if it would increase the size of the UNSUBACK packet beyond the Maximum Packet Size
             * specified by the Client [MQTT-3.11.2-1]. It is a Protocol Error to include the Reason String more
             * than once.
             */
            val reasonString: MqttUtf8String? = null,
            /**
             * 3.11.2.1.3 User Property
             *
             * 38 (0x26) Byte, Identifier of the User Property.
             *
             * Followed by UTF-8 String Pair. This property can be used to provide additional diagnostic or
             * other information. The Server MUST NOT send this property if it would increase the size of the
             * UNSUBACK packet beyond the Maximum Packet Size specified by the Client [MQTT-3.11.2-2]. The User
             * Property is allowed to appear multiple times to represent multiple name, value pairs. The same
             * name is allowed to appear more than once.
             */
            val userProperty: List<Pair<MqttUtf8String, MqttUtf8String>> = emptyList()
        ) : Parcelable {
            @IgnoredOnParcel val packet by lazy {
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
                buildPacket {
                    writePacket(VariableByteInteger(propertyLength.toUInt()).encodedValue())
                    writePacket(propertiesPacket)
                }
            }

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
                    var reasonString: MqttUtf8String? = null
                    val userProperty = mutableListOf<Pair<MqttUtf8String, MqttUtf8String>>()
                    keyValuePairs?.forEach {
                        when (it) {
                            is ReasonString -> {
                                if (reasonString != null) {
                                    throw ProtocolError(
                                        "Reason String added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477476")
                                }
                                reasonString = it.diagnosticInfoDontParse
                            }
                            is UserProperty -> userProperty += Pair(it.key, it.value)
                            else -> throw MalformedPacketException("Invalid UnsubscribeAck property type found in MQTT properties $it")
                        }
                    }
                    return Properties(reasonString, userProperty)
                }
            }
        }

        companion object {
            fun from(buffer: ByteReadPacket): VariableHeader {
                val packetIdentifier = buffer.readUShort()
                val props = Properties.from(buffer.readPropertiesLegacy())
                return VariableHeader(packetIdentifier.toInt(), props)
            }

            fun from(buffer: ReadBuffer): Pair<UInt, VariableHeader> {
                val packetIdentifier = buffer.readUnsignedShort()
                val sized = buffer.readPropertiesSized()
                val props = Properties.from(sized.second)
                return Pair(
                    UShort.SIZE_BYTES.toUInt() + buffer.variableByteSize(sized.first) + sized.first,
                    VariableHeader(packetIdentifier.toInt(), props)
                )
            }
        }
    }

    companion object {
        fun from(buffer: ByteReadPacket): UnsubscribeAcknowledgment {
            val variableHeader = VariableHeader.from(buffer)
            val list = mutableListOf<ReasonCode>()
            while (buffer.remaining > 0) {
                val reasonCodeByte = buffer.readUByte()
                list += when (reasonCodeByte) {
                    SUCCESS.byte -> SUCCESS
                    NO_SUBSCRIPTIONS_EXISTED.byte -> NO_SUBSCRIPTIONS_EXISTED
                    UNSPECIFIED_ERROR.byte -> UNSPECIFIED_ERROR
                    IMPLEMENTATION_SPECIFIC_ERROR.byte -> IMPLEMENTATION_SPECIFIC_ERROR
                    NOT_AUTHORIZED.byte -> NOT_AUTHORIZED
                    TOPIC_FILTER_INVALID.byte -> TOPIC_FILTER_INVALID
                    PACKET_IDENTIFIER_IN_USE.byte -> PACKET_IDENTIFIER_IN_USE
                    else -> throw MalformedPacketException(
                        "Invalid reason code $reasonCodeByte " +
                                "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477478"
                    )
                }
            }
            return UnsubscribeAcknowledgment(variableHeader, list)
        }

        fun from(buffer: ReadBuffer, remainingLength: UInt): UnsubscribeAcknowledgment {
            val variableHeader = VariableHeader.from(buffer)
            val list = mutableListOf<ReasonCode>()
            while (remainingLength - variableHeader.first > list.count().toUInt()) {
                val reasonCodeByte = buffer.readUnsignedByte()
                list += when (reasonCodeByte) {
                    SUCCESS.byte -> SUCCESS
                    NO_SUBSCRIPTIONS_EXISTED.byte -> NO_SUBSCRIPTIONS_EXISTED
                    UNSPECIFIED_ERROR.byte -> UNSPECIFIED_ERROR
                    IMPLEMENTATION_SPECIFIC_ERROR.byte -> IMPLEMENTATION_SPECIFIC_ERROR
                    NOT_AUTHORIZED.byte -> NOT_AUTHORIZED
                    TOPIC_FILTER_INVALID.byte -> TOPIC_FILTER_INVALID
                    PACKET_IDENTIFIER_IN_USE.byte -> PACKET_IDENTIFIER_IN_USE
                    else -> throw MalformedPacketException(
                        "Invalid reason code $reasonCodeByte " +
                                "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477478"
                    )
                }
            }
            return UnsubscribeAcknowledgment(variableHeader.second, list)
        }
    }
}

private val validSubscribeCodes by lazy {
    setOf(SUCCESS,
            NO_SUBSCRIPTIONS_EXISTED,
            UNSPECIFIED_ERROR,
            IMPLEMENTATION_SPECIFIC_ERROR,
            NOT_AUTHORIZED,
            TOPIC_FILTER_INVALID,
            PACKET_IDENTIFIER_IN_USE)
}
