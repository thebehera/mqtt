@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.control.packet.format.variable.property.Property
import mqtt.wire.control.packet.format.variable.property.ReasonString
import mqtt.wire.control.packet.format.variable.property.UserProperty
import mqtt.wire.control.packet.format.variable.property.readProperties
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VariableByteInteger

data class UnsubscribeAcknowledgment(val variable: VariableHeader, val reasonCodes: List<ReasonCode> = listOf(SUCCESS))
    : ControlPacket(11, DirectionOfFlow.SERVER_TO_CLIENT) {
    override val variableHeaderPacket: ByteReadPacket = variable.packet
    override fun payloadPacket(sendDefaults: Boolean) = buildPacket { reasonCodes.forEach { writeUByte(it.byte) } }

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
    data class VariableHeader(val packetIdentifier: UShort,
                              val properties: Properties = Properties()) {
        val packet by lazy {
            buildPacket {
                writeUShort(packetIdentifier)
                writePacket(properties.packet)
            }
        }

        /**
         * 3.9.2.1 SUBACK Properties
         */
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
                val userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = emptyList()) {
            val packet by lazy {
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

            companion object {
                fun from(keyValuePairs: Collection<Property>?): Properties {
                    var reasonString: MqttUtf8String? = null
                    var userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = mutableListOf()
                    keyValuePairs?.forEach {
                        when (it) {
                            is ReasonString -> {
                                if (reasonString != null) {
                                    throw ProtocolError("Reason String added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477476")
                                }
                                reasonString = it.diagnosticInfoDontParse
                            }
                            is UserProperty -> userProperty += Pair(it.key, it.value)
                        }
                    }
                    return Properties(reasonString, userProperty)
                }
            }
        }

        companion object {
            fun from(buffer: ByteReadPacket): VariableHeader {
                val packetIdentifier = buffer.readUShort()
                val props = Properties.from(buffer.readProperties())
                return VariableHeader(packetIdentifier, props)
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
                    else -> throw MalformedPacketException("Invalid reason code $reasonCodeByte " +
                            "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477478")
                }
            }
            return UnsubscribeAcknowledgment(variableHeader, list)
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