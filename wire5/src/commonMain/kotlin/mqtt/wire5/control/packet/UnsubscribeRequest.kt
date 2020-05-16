@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.IgnoredOnParcel
import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.IUnsubscribeRequest
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire5.control.packet.format.variable.property.Property
import mqtt.wire5.control.packet.format.variable.property.UserProperty
import mqtt.wire5.control.packet.format.variable.property.readPropertiesSized

/**
 * 3.10 UNSUBSCRIBE – Unsubscribe request
 * An UNSUBSCRIBE packet is sent by the Client to the Server, to unsubscribe from topics.
 */
@Parcelize
data class UnsubscribeRequest(val variable: VariableHeader, val topics: Set<CharSequence>) :
    ControlPacketV5(10, DirectionOfFlow.CLIENT_TO_SERVER, 0b10), IUnsubscribeRequest {

    override fun variableHeader(writeBuffer: WriteBuffer) = variable.serialize(writeBuffer)
    override fun remainingLength(buffer: WriteBuffer): UInt {
        val variableSize = variable.size(buffer)
        var payloadSize = 0u
        topics.forEach { payloadSize += UShort.SIZE_BYTES.toUInt() + buffer.mqttUtf8Size(it) }
        return variableSize + payloadSize
    }

    override fun payload(writeBuffer: WriteBuffer) = topics.forEach { writeBuffer.writeUtf8String(it) }

    constructor(packetIdentifier: Int, topics: Set<CharSequence>) : this(VariableHeader(packetIdentifier), topics)

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
        fun size(writeBuffer: WriteBuffer) =
            UShort.SIZE_BYTES.toUInt() + writeBuffer.variableByteIntegerSize(properties.size(writeBuffer)) + properties.size(
                writeBuffer
            )

        fun serialize(writeBuffer: WriteBuffer) {
            writeBuffer.write(packetIdentifier.toUShort())
            properties.serialize(writeBuffer)
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
            val userProperty: List<Pair<CharSequence, CharSequence>> = emptyList()
        ) : Parcelable {
            @IgnoredOnParcel
            val props by lazy {
                val props = ArrayList<Property>(userProperty.size)
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
                    val userProperty = mutableListOf<Pair<CharSequence, CharSequence>>()
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
            fun from(buffer: ReadBuffer): Pair<UInt, VariableHeader> {
                val packetIdentifier = buffer.readUnsignedShort().toInt()
                val sized = buffer.readPropertiesSized()
                val props = Properties.from(sized.second)
                return Pair(
                    sized.first + buffer.variableByteSize(sized.first) + UShort.SIZE_BYTES.toUInt(),
                    VariableHeader(packetIdentifier, props)
                )
            }
        }
    }

    companion object {
        fun from(buffer: ReadBuffer, remainingLength: UInt): UnsubscribeRequest {
            val header = VariableHeader.from(buffer)
            val topics = mutableSetOf<CharSequence>()
            var bytesRead = header.first
            while (bytesRead < remainingLength) {
                val result = buffer.readMqttUtf8StringNotValidatedSized()
                bytesRead += result.first + UShort.SIZE_BYTES.toUInt()
                topics += result.second
            }
            return UnsubscribeRequest(header.second, topics)
        }
    }
}
