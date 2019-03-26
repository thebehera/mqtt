@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUByte
import kotlinx.io.core.writeUByte
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.VariableByteInteger
import mqtt.wire5.control.packet.format.ReasonCode
import mqtt.wire5.control.packet.format.ReasonCode.*
import mqtt.wire5.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire5.control.packet.format.variable.property.*

/**
 * 3.14 DISCONNECT – Disconnect notification
 *
 * The DISCONNECT packet is the final MQTT Control Packet sent from the Client or the Server. It indicates the reason
 * why the Network Connection is being closed. The Client or Server MAY send a DISCONNECT packet before closing the
 * Network Connection. If the Network Connection is closed without the Client first sending a DISCONNECT packet with
 * Reason Code 0x00 (Normal disconnection) and the Connection has a Will Message, the Will Message is published. Refer
 * to section 3.1.2.5 for further details.
 *
 * A Server MUST NOT send a DISCONNECT until after it has sent a CONNACK with Reason Code of less than 0x80
 * [MQTT-3.14.0-1].
 */
data class DisconnectNotification(val variable: VariableHeader = VariableHeader()) : ControlPacket(14, DirectionOfFlow.BIDIRECTIONAL) {
    override val variableHeaderPacket = variable.packet

    data class VariableHeader(val reasonCode: ReasonCode = NORMAL_DISCONNECTION, val properties: Properties = Properties()) {
        init {
            // throw if the reason code is not valid for the disconnect notification
            getDisconnectCode(reasonCode.byte)
        }

        val packet by lazy {
            buildPacket {
                writeUByte(reasonCode.byte)
                writePacket(properties.packet)
            }
        }

        data class Properties(
                /**
                 * 3.14.2.2.2 Session Expiry Interval
                 *
                 * 17 (0x11) Byte, Identifier of the Session Expiry Interval.
                 *
                 * Followed by the Four Byte Integer representing the Session Expiry Interval in seconds. It is a
                 * Protocol Error to include the Session Expiry Interval more than once.
                 *
                 * If the Session Expiry Interval is absent, the Session Expiry Interval in the CONNECT packet is used.
                 *
                 * The Session Expiry Interval MUST NOT be sent on a DISCONNECT by the Server [MQTT-3.14.2-2].
                 *
                 * If the Session Expiry Interval in the CONNECT packet was zero, then it is a Protocol Error to set a
                 * non-zero Session Expiry Interval in the DISCONNECT packet sent by the Client. If such a non-zero
                 * Session Expiry Interval is received by the Server, it does not treat it as a valid DISCONNECT
                 * packet. The Server uses DISCONNECT with Reason Code 0x82 (Protocol Error) as described in
                 * section 4.13.
                 */
                val sessionExpiryIntervalSeconds: UInt? = null,
                /**
                 * 3.14.2.2.3 Reason String
                 *
                 * 31 (0x1F) Byte, Identifier of the Reason String.
                 *
                 * Followed by the UTF-8 Encoded String representing the reason for the disconnect. This Reason
                 * String is human readable, designed for diagnostics and SHOULD NOT be parsed by the receiver.
                 *
                 * The sender MUST NOT send this Property if it would increase the size of the DISCONNECT packet
                 * beyond the Maximum Packet Size specified by the receiver [MQTT-3.14.2-3]. It is a Protocol Error
                 * to include the Reason String more than once.
                 */
                val reasonString: MqttUtf8String? = null,
                /**
                 * 3.14.2.2.4 User Property
                 *
                 * 38 (0x26) Byte, Identifier of the User Property.
                 *
                 * Followed by UTF-8 String Pair. This property may be used to provide additional diagnostic or other
                 * information. The sender MUST NOT send this property if it would increase the size of the DISCONNECT
                 * packet beyond the Maximum Packet Size specified by the receiver [MQTT-3.14.2-4]. The User Property
                 * is allowed to appear multiple times to represent multiple name, value pairs. The same name is
                 * allowed to appear more than once.
                 */
                val userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = emptyList(),
                /**
                 * 3.14.2.2.5 Server Reference
                 *
                 * 28 (0x1C) Byte, Identifier of the Server Reference.
                 *
                 * Followed by a UTF-8 Encoded String which can be used by the Client to identify another Server to
                 * use. It is a Protocol Error to include the Server Reference more than once.
                 *
                 * The Server sends DISCONNECT including a Server Reference and Reason Code 0x9C (Use another server)
                 * or 0x9D (Server moved) as described in section 4.13.
                 *
                 * Refer to section 4.11 Server Redirection for information about how Server Reference is used.
                 */
                val serverReference: MqttUtf8String? = null) {
            val packet by lazy {
                val propertiesPacket = buildPacket {
                    if (sessionExpiryIntervalSeconds != null) {
                        SessionExpiryInterval(sessionExpiryIntervalSeconds).write(this)
                    }
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
                    if (serverReference != null) {
                        ServerReference(serverReference).write(this)
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
                    var sessionExpiryIntervalSeconds: UInt? = null
                    var reasonString: MqttUtf8String? = null
                    var userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = mutableListOf()
                    var serverReference: MqttUtf8String? = null
                    keyValuePairs?.forEach {
                        when (it) {
                            is SessionExpiryInterval -> {
                                if (sessionExpiryIntervalSeconds != null) {
                                    throw ProtocolError("Session Expiry Interval added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477382")
                                }
                                sessionExpiryIntervalSeconds = it.seconds
                            }
                            is ReasonString -> {
                                if (reasonString != null) {
                                    throw ProtocolError("Reason String added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477476")
                                }
                                reasonString = it.diagnosticInfoDontParse
                            }
                            is UserProperty -> userProperty += Pair(it.key, it.value)
                            is ServerReference -> {
                                if (serverReference != null) {
                                    throw ProtocolError("Server Reference added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477396")
                                }
                                serverReference = it.otherServer
                            }
                            else -> throw MalformedPacketException("Invalid UnsubscribeAck property type found in MQTT properties $it")
                        }
                    }
                    return Properties(sessionExpiryIntervalSeconds, reasonString, userProperty, serverReference)
                }
            }
        }

        companion object {
            fun from(buffer: ByteReadPacket): VariableHeader {
                val reasonCodeByte = buffer.readUByte()
                val reasonCode = getDisconnectCode(reasonCodeByte)
                val props = Properties.from(buffer.readProperties())
                return VariableHeader(reasonCode, props)
            }
        }
    }

    companion object {
        fun from(buffer: ByteReadPacket): DisconnectNotification {
            val variableHeader = VariableHeader.from(buffer)
            return DisconnectNotification(variableHeader)
        }
    }
}

private fun getDisconnectCode(byte: UByte): ReasonCode {
    return when (byte) {
        NORMAL_DISCONNECTION.byte -> NORMAL_DISCONNECTION
        DISCONNECT_WITH_WILL_MESSAGE.byte -> DISCONNECT_WITH_WILL_MESSAGE
        UNSPECIFIED_ERROR.byte -> UNSPECIFIED_ERROR
        MALFORMED_PACKET.byte -> MALFORMED_PACKET
        PROTOCOL_ERROR.byte -> PROTOCOL_ERROR
        IMPLEMENTATION_SPECIFIC_ERROR.byte -> IMPLEMENTATION_SPECIFIC_ERROR
        NOT_AUTHORIZED.byte -> NOT_AUTHORIZED
        SERVER_BUSY.byte -> SERVER_BUSY
        SERVER_SHUTTING_DOWN.byte -> SERVER_SHUTTING_DOWN
        KEEP_ALIVE_TIMEOUT.byte -> KEEP_ALIVE_TIMEOUT
        SESSION_TAKE_OVER.byte -> SESSION_TAKE_OVER
        TOPIC_FILTER_INVALID.byte -> TOPIC_FILTER_INVALID
        TOPIC_NAME_INVALID.byte -> TOPIC_NAME_INVALID
        RECEIVE_MAXIMUM_EXCEEDED.byte -> RECEIVE_MAXIMUM_EXCEEDED
        TOPIC_ALIAS_INVALID.byte -> TOPIC_ALIAS_INVALID
        PACKET_TOO_LARGE.byte -> PACKET_TOO_LARGE
        MESSAGE_RATE_TOO_HIGH.byte -> MESSAGE_RATE_TOO_HIGH
        QUOTA_EXCEEDED.byte -> QUOTA_EXCEEDED
        ADMINISTRATIVE_ACTION.byte -> ADMINISTRATIVE_ACTION
        PAYLOAD_FORMAT_INVALID.byte -> PAYLOAD_FORMAT_INVALID
        RETAIN_NOT_SUPPORTED.byte -> RETAIN_NOT_SUPPORTED
        QOS_NOT_SUPPORTED.byte -> QOS_NOT_SUPPORTED
        USE_ANOTHER_SERVER.byte -> USE_ANOTHER_SERVER
        SERVER_MOVED.byte -> SERVER_MOVED
        SHARED_SUBSCRIPTIONS_NOT_SUPPORTED.byte -> SHARED_SUBSCRIPTIONS_NOT_SUPPORTED
        CONNECTION_RATE_EXCEEDED.byte -> CONNECTION_RATE_EXCEEDED
        MAXIMUM_CONNECTION_TIME.byte -> MAXIMUM_CONNECTION_TIME
        SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED.byte -> SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED
        WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED.byte -> WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED
        else -> throw MalformedPacketException("Invalid disconnect reason code $byte")
    }
}
