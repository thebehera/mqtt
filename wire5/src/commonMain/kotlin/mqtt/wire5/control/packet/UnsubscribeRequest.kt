@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUShort
import kotlinx.io.core.writeUShort
import mqtt.IgnoredOnParcel
import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.IUnsubscribeRequest
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VariableByteInteger
import mqtt.wire.data.readMqttUtf8String
import mqtt.wire.data.writeMqttUtf8String
import mqtt.wire5.control.packet.format.variable.property.Property
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties

/**
 * 3.10 UNSUBSCRIBE – Unsubscribe request
 * An UNSUBSCRIBE packet is sent by the Client to the Server, to unsubscribe from topics.
 */
@Parcelize
data class UnsubscribeRequest(val variable: VariableHeader, val topics: Set<MqttUtf8String>)
    : ControlPacketV5(10, DirectionOfFlow.CLIENT_TO_SERVER, 0b10), IUnsubscribeRequest {
    @IgnoredOnParcel
    override val variableHeaderPacket: ByteReadPacket = variable.packet
    override fun payloadPacket(sendDefaults: Boolean) = buildPacket { topics.forEach { writeMqttUtf8String(it) } }

    init {
        if (topics.isEmpty()) {
            throw ProtocolError("An UNSUBSCRIBE packet with no Payload is a Protocol Error")
        }
    }

    /**
     * 3.10.2 UNSUBSCRIBE Variable Header
     *
     * The Variable Header of the UNSUBSCRIBE Packet contains the following fields in the order: Packet Identifier,
     * and Properties. Section 2.2.1 provides more information about Packet Identifiers. The rules for encoding
     * Properties are described in section 2.2.2.
     */
    @Parcelize
    data class VariableHeader(
        val packetIdentifier: Int,
        val properties: Properties = Properties()
    ) : Parcelable {
        @IgnoredOnParcel val packet by lazy {
            buildPacket {
                writeUShort(packetIdentifier.toUShort())
                writePacket(properties.packet)
            }
        }

        /**
         * 3.10.2.1 UNSUBSCRIBE Properties
         */
        @Parcelize
        data class Properties(
                /**
                 * 3.10.2.1.2 User Property
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
                 * User Properties on the UNSUBSCRIBE packet can be used to send subscription related properties from
                 * the Client to the Server. The meaning of these properties is not defined by this specification.
                 */
                val userProperty: List<Pair<MqttUtf8String, MqttUtf8String>> = emptyList()
        ) : Parcelable {
            @IgnoredOnParcel val packet by lazy {
                val propertiesPacket = buildPacket {
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
                    val userProperty = mutableListOf<Pair<MqttUtf8String, MqttUtf8String>>()
                    keyValuePairs?.forEach {
                        when (it) {
                            is UserProperty -> userProperty += Pair(it.key, it.value)
                            else -> throw MalformedPacketException("Invalid Unsubscribe Request property type found in MQTT properties $it")
                        }
                    }
                    return Properties(userProperty)
                }
            }
        }

        companion object {
            fun from(buffer: ByteReadPacket): VariableHeader {
                val packetIdentifier = buffer.readUShort().toInt()
                val props = Properties.from(buffer.readProperties())
                return VariableHeader(packetIdentifier, props)
            }
        }
    }

    companion object {
        fun from(buffer: ByteReadPacket): UnsubscribeRequest {
            val header = VariableHeader.from(buffer)
            val topics = mutableSetOf<MqttUtf8String>()
            while (buffer.remaining > 0) {
                topics += buffer.readMqttUtf8String()
            }
            return UnsubscribeRequest(header, topics)
        }
    }
}
