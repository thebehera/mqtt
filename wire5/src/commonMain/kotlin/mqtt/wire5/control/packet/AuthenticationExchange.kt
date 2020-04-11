@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import mqtt.Parcelable
import mqtt.Parcelize
import mqtt.buffer.ReadBuffer
import mqtt.buffer.WriteBuffer
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.MqttUtf8String
import mqtt.wire5.control.packet.format.variable.property.*

/**
 * 3.15 AUTH – Authentication exchange
 * An AUTH packet is sent from Client to Server or Server to Client as part of an extended authentication exchange,
 * such as challenge / response authentication. It is a Protocol Error for the Client or Server to send an AUTH packet
 * if the CONNECT packet did not contain the same Authentication Method.
 *
 * Bits 3,2,1 and 0 of the Fixed Header of the AUTH packet are reserved and MUST all be set to 0. The Client or Server
 * MUST treat any other value as malformed and close the Network Connection [MQTT-3.15.1-1].
 */
@Parcelize
data class AuthenticationExchange(val variable: VariableHeader)
    : ControlPacketV5(15, DirectionOfFlow.BIDIRECTIONAL) {

    override fun remainingLength(buffer: WriteBuffer) = variable.size(buffer)
    override fun variableHeader(writeBuffer: WriteBuffer) = variable.serialize(writeBuffer)

    /**
     * 3.15.2 AUTH Variable Header
     *
     * The Variable Header of the AUTH Packet contains the following fields in the order: Authenticate Reason Code,
     * and Properties. The rules for encoding Properties are described in section 2.2.2.
     *
     * The Reason Code and Property Length can be omitted if the Reason Code is 0x00 (Success) and there are no
     * Properties. In this case the AUTH has a Remaining Length of 0.
     */
    @Parcelize
    data class VariableHeader(
        /**
             * 3.15.2.1 Authenticate Reason Code
             *
             * Byte 0 in the Variable Header is the Authenticate Reason Code. The values for the one byte unsigned
             * Authenticate Reason Code field are shown below. The sender of the AUTH Packet MUST use one of the
             * Authenticate Reason Codes [MQTT-3.15.2-1].
             *
             * Value |Hex|Reason Code name|Sent by|Description
             *
             * 0|0x00|Success|Server|Authentication is successful
             *
             * 24|0x18|Continue authentication|Client or Server|Continue the authentication with another step
             *
             * 25|0x19|Re-authenticate|Client
             */
            val reasonCode: ReasonCode = SUCCESS,
        val properties: Properties
    ) : Parcelable {
        init {
            // throw if reason code doesnt exist
            getReasonCode(reasonCode.byte)
        }

        fun size(writeBuffer: WriteBuffer) = UByte.SIZE_BYTES.toUInt() + properties.size(writeBuffer)

        fun serialize(writeBuffer: WriteBuffer) {
            writeBuffer.write(reasonCode.byte)
            properties.serialize(writeBuffer)
        }

        @Parcelize
        data class Properties(
            val method: MqttUtf8String,
            val data: ByteArrayWrapper? = null,
            val reasonString: MqttUtf8String? = null,
            val userProperty: List<Pair<MqttUtf8String, MqttUtf8String>> = emptyList()
        ) : Parcelable {

            fun size(writeBuffer: WriteBuffer): UInt {
                val authMethod = AuthenticationMethod(method)
                val authData = if (data != null) AuthenticationData(data) else null
                val authReasonString = if (reasonString != null) ReasonString(reasonString) else null
                val props = userProperty.map { UserProperty(it.first, it.second) }
                var size = authMethod.size(writeBuffer)
                size += authData?.size(writeBuffer) ?: 0.toUInt()
                size += authReasonString?.size(writeBuffer) ?: 0.toUInt()
                props.forEach {
                    size += it.size(writeBuffer)
                }
                size += writeBuffer.variableByteIntegerSize(size)
                return size
            }

            fun serialize(writeBuffer: WriteBuffer) {
                val authMethod = AuthenticationMethod(method)
                val authData = if (data != null) AuthenticationData(data) else null
                val authReasonString = if (reasonString != null) ReasonString(reasonString) else null
                val props = userProperty.map { UserProperty(it.first, it.second) }
                var size = size(writeBuffer)
                writeBuffer.writeVariableByteInteger(size)
                authMethod.write(writeBuffer)
                authData?.write(writeBuffer)
                authReasonString?.write(writeBuffer)
                props.forEach {
                    it.write(writeBuffer)
                }
            }

            companion object {
                fun from(keyValuePairs: Collection<Property>?): Properties {
                    var method: MqttUtf8String? = null
                    var reasonString: MqttUtf8String? = null
                    val userProperty = mutableListOf<Pair<MqttUtf8String, MqttUtf8String>>()
                    var data: ByteArrayWrapper? = null
                    keyValuePairs?.forEach {
                        when (it) {
                            is AuthenticationMethod -> {
                                if (method != null) {
                                    throw ProtocolError("Auth Method added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477382")
                                }
                                method = it.value
                            }
                            is ReasonString -> {
                                if (reasonString != null) {
                                    throw ProtocolError(
                                        "Reason String added multiple times see: " +
                                                "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477476"
                                    )
                                }
                                reasonString = it.diagnosticInfoDontParse
                            }
                            is UserProperty -> userProperty.add(Pair(it.key, it.value))
                            is AuthenticationData -> {
                                if (data != null) {
                                    throw ProtocolError(
                                        "Server Reference added multiple times see: " +
                                                "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477396"
                                    )
                                }
                                data = it.data
                            }
                            else -> throw MalformedPacketException("Invalid UnsubscribeAck property type found in MQTT properties $it")
                        }
                    }
                    return Properties(method!!, data, reasonString, userProperty)
                }
            }
        }

        companion object {
            fun from(buffer: ReadBuffer): VariableHeader {
                val reasonCodeByte = buffer.readUnsignedByte()
                val reasonCode = getReasonCode(reasonCodeByte)
                val props = Properties.from(buffer.readProperties())
                return VariableHeader(reasonCode, props)
            }
        }
    }

    companion object {
        fun from(buffer: ReadBuffer) = AuthenticationExchange(VariableHeader.from(buffer))

    }
}

private fun getReasonCode(byte: UByte): ReasonCode {
    return when (byte) {
        SUCCESS.byte -> SUCCESS
        CONTINUE_AUTHENTICATION.byte -> CONTINUE_AUTHENTICATION
        REAUTHENTICATE.byte -> REAUTHENTICATE
        else -> throw MalformedPacketException("Invalid disconnect reason code $byte")
    }
}
