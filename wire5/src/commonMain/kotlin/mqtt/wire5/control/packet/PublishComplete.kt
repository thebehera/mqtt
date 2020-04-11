@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.IgnoredOnParcel
import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.IPublishComplete
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.ReasonCode.PACKET_IDENTIFIER_NOT_FOUND
import mqtt.wire.control.packet.format.ReasonCode.SUCCESS
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.MqttUtf8String
import mqtt.wire5.control.packet.format.variable.property.Property
import mqtt.wire5.control.packet.format.variable.property.ReasonString
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readProperties

/**
 * 3.7 PUBCOMP – Publish complete (QoS 2 delivery part 3)
 *
 * The PUBCOMP packet is the response to a PUBREL packet. It is the fourth and final packet of the QoS 2 protocol exchange.
 */
@Parcelize
data class PublishComplete(val variable: VariableHeader) :
        ControlPacketV5(7, DirectionOfFlow.BIDIRECTIONAL), IPublishComplete {
    constructor(packetIdentifier: UShort, reasonCode: ReasonCode = SUCCESS)
            : this(VariableHeader(packetIdentifier.toInt(), reasonCode))

    override fun variableHeader(writeBuffer: WriteBuffer) = variable.serialize(writeBuffer)
    @IgnoredOnParcel
    override val packetIdentifier = variable.packetIdentifier
    override fun remainingLength(buffer: WriteBuffer) = variable.size(buffer)

    /**
     * 3.7.2 PUBCOMP Variable Header
     *
     * The Variable Header of the PUBCOMP Packet contains the following fields in the order: Packet Identifier from
     * the PUBREL packet that is being acknowledged, PUBCOMP Reason Code, and Properties. The rules for encoding
     * Properties are described in section 2.2.2.
     */
    @Parcelize
    data class VariableHeader(
        val packetIdentifier: Int,
        /**
         * 3.7.2.1 PUBCOMP Reason Code
         *
         * Byte 3 in the Variable Header is the PUBCOMP Reason Code. If the Remaining Length is
         * 2, then the value 0x00 (Success) is used.
         *
         * The Client or Server sending the PUBCOMP packet MUST use one of the PUBCOMP Reason
         * Code values [MQTT-3.7.2-1]. The Reason Code and Property Length can be omitted if the
         * Reason Code is 0x00 (Success) and there are no Properties. In this case the PUBCOMP
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
                0, 0x92 -> {
                }
                else -> throw ProtocolError(
                    "Invalid Publish Acknowledgment reason code ${reasonCode.byte} " +
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

        fun serialize(buffer: WriteBuffer) {
            val canOmitReasonCodeAndProperties = (reasonCode == SUCCESS
                    && properties.userProperty.isEmpty()
                    && properties.reasonString == null)
            buffer.write(packetIdentifier.toUShort())
            if (!canOmitReasonCodeAndProperties) {
                buffer.write(reasonCode.byte)
                properties.serialize(buffer)
            }
        }

        @Parcelize
        data class Properties(
            /**
             * 3.7.2.2.2 Reason String
             *
             * 31 (0x1F) Byte, Identifier of the Reason String.
             *
             * Followed by the UTF-8 Encoded String representing the reason associated with this response. This
             * Reason String is a human readable string designed for diagnostics and SHOULD NOT be parsed by the
             * receiver.
             *
             * The sender uses this value to give additional information to the receiver. The sender MUST NOT send
             * this Property if it would increase the size of the PUBCOMP packet beyond the Maximum Packet Size
             * specified by the receiver [MQTT-3.7.2-2]. It is a Protocol Error to include the Reason String more
             * than once.
             */
            val reasonString: MqttUtf8String? = null,
            /**
             * 3.7.2.2.3 User Property
             *
             * 38 (0x26) Byte, Identifier of the User Property.
             *
             * Followed by UTF-8 String Pair. This property can be used to provide additional diagnostic or other
             * information. The sender MUST NOT send this property if it would increase the size of the PUBCOMP
             * packet beyond the Maximum Packet Size specified by the receiver [MQTT-3.7.2-3]. The User Property
             * is allowed to appear multiple times to represent multiple name, value pairs. The same name is
             * allowed to appear more than once.
             */
            val userProperty: List<Pair<MqttUtf8String, MqttUtf8String>> = emptyList()
        ) : Parcelable {

            val props by lazy {
                val list = ArrayList<Property>(1 + userProperty.count())
                if (reasonString != null) {
                    list += ReasonString(reasonString)
                }
                if (userProperty.isNotEmpty()) {
                    for (keyValueProperty in userProperty) {
                        val key = keyValueProperty.first
                        val value = keyValueProperty.second
                        list += UserProperty(key, value)
                    }
                }
                list
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
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477427")
                                }
                                reasonString = it.diagnosticInfoDontParse
                            }
                            is UserProperty -> userProperty += Pair(it.key, it.value)
                            else -> throw MalformedPacketException("Invalid Publish Complete property type found in MQTT properties $it")
                        }
                    }
                    return Properties(reasonString, userProperty)
                }
            }
        }

        companion object {
            fun from(buffer: ReadBuffer, remainingLength: UInt): VariableHeader {
                val packetIdentifier = buffer.readUnsignedShort()
                return if (remainingLength == 2u) {
                    VariableHeader(packetIdentifier.toInt())
                } else {
                    val reasonCodeByte = buffer.readUnsignedByte()
                    val reasonCode = when (reasonCodeByte) {
                        SUCCESS.byte -> SUCCESS
                        PACKET_IDENTIFIER_NOT_FOUND.byte -> PACKET_IDENTIFIER_NOT_FOUND
                        else -> throw MalformedPacketException(
                            "Invalid reason code $reasonCodeByte" +
                                    "see: https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477444"
                        )
                    }
                    val propsData = buffer.readProperties()
                    val props = Properties.from(propsData)
                    VariableHeader(packetIdentifier.toInt(), reasonCode, props)
                }
            }
        }
    }

    companion object {
        fun from(buffer: ReadBuffer, remainingLength: UInt) =
            PublishComplete(VariableHeader.from(buffer, remainingLength))
    }
}
