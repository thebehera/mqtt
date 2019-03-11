@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readUByte
import kotlinx.io.core.writeUByte
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.control.packet.format.ReasonCode.*
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.control.packet.format.variable.property.*
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.VariableByteInteger

typealias CONNACK = ConnectionAcknowledgment

data class ConnectionAcknowledgment(val header: VariableHeader = VariableHeader())
    : ControlPacket(2, DirectionOfFlow.SERVER_TO_CLIENT) {

    override val variableHeaderPacket: ByteReadPacket = header.packet()

    data class VariableHeader(
            val sessionPresent: Boolean = false,
            val connectReason: ReasonCode = SUCCESS,
            val properties: Properties = Properties()) {
        data class Properties(
                val sessionExpiryIntervalSeconds: UInt? = null,
                val receiveMaximum: UShort = UShort.MAX_VALUE,
                val maximumQos: QualityOfService = QualityOfService.EXACTLY_ONCE,
                val retainAvailable: Boolean = true,
                val maximumPacketSize: UInt? = null,
                val assignedClientIdentifier: MqttUtf8String? = null,
                val topicAliasMaximum: UShort = 0.toUShort(),
                val reasonString: MqttUtf8String? = null,
                val userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = emptyList(),
                val supportsWildcardSubscriptions: Boolean = true,
                val subscriptionIdentifiersAvailable: Boolean = true,
                val sharedSubscriptionAvailable: Boolean = true,
                val serverKeepAlive: UShort? = null,
                val responseInformation: MqttUtf8String? = null,
                val serverReference: MqttUtf8String? = null,
                val authentication: Authentication? = null) {
            fun packet(sendDefaults: Boolean = false): ByteReadPacket {
                val propertiesPacket = buildPacket {
                    if (sessionExpiryIntervalSeconds != null) {
                        SessionExpiryInterval(sessionExpiryIntervalSeconds).write(this)
                    }
                    if (receiveMaximum != UShort.MAX_VALUE || sendDefaults) {
                        ReceiveMaximum(receiveMaximum).write(this)
                    }
                    if (maximumQos != QualityOfService.EXACTLY_ONCE || sendDefaults) {
                        MaximumQos(maximumQos).write(this)
                    }
                    if (!retainAvailable || sendDefaults) {
                        RetainAvailable(retainAvailable).write(this)
                    }
                    if (maximumPacketSize != null) {
                        MaximumPacketSize(maximumPacketSize).write(this)
                    }
                    if (assignedClientIdentifier != null) {
                        AssignedClientIdentifier(assignedClientIdentifier).write(this)
                    }
                    if (topicAliasMaximum == 0.toUShort() || sendDefaults) {
                        TopicAliasMaximum(topicAliasMaximum).write(this)
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
                    if (!supportsWildcardSubscriptions || sendDefaults) {
                        WildcardSubscriptionAvailable(supportsWildcardSubscriptions).write(this)
                    }
                    if (!subscriptionIdentifiersAvailable || sendDefaults) {
                        SubscriptionIdentifierAvailable(subscriptionIdentifiersAvailable).write(this)
                    }
                    if (!sharedSubscriptionAvailable || sendDefaults) {
                        SharedSubscriptionAvailable(sharedSubscriptionAvailable).write(this)
                    }
                    if (serverKeepAlive != null) {
                        ServerKeepAlive(serverKeepAlive).write(this)
                    }
                    if (responseInformation != null) {
                        ResponseInformation(responseInformation).write(this)
                    }
                    if (serverReference != null) {
                        ServerReference(serverReference).write(this)
                    }
                    if (authentication != null) {
                        AuthenticationMethod(authentication.method).write(this)
                        AuthenticationData(authentication.data).write(this)
                    }
                }
                // The length of the Properties in the CONNECT packet Variable Header encoded as a
                // Variable Byte Integer.
                val propertyLength = propertiesPacket.remaining
                return buildPacket {
                    writePacket(VariableByteInteger(propertyLength.toUInt()).encodedValue())
                    writePacket(propertiesPacket)
                }
            }

            data class Authentication(
                    val method: MqttUtf8String,
                    val data: ByteArrayWrapper)

            companion object {
                fun from(keyValuePairs: Collection<Property>?): Properties {
                    var sessionExpiryIntervalSeconds: UInt? = null
                    var receiveMaximum: UShort? = null
                    var maximumQos: QualityOfService? = null
                    var retainAvailable: Boolean? = null
                    var maximumPacketSize: UInt? = null
                    var assignedClientIdentifier: MqttUtf8String? = null
                    var topicAliasMaximum: UShort? = null
                    var reasonString: MqttUtf8String? = null
                    var userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = mutableListOf()
                    var supportsWildcardSubscriptions: Boolean? = null
                    var subscriptionIdentifiersAvailable: Boolean? = null
                    var sharedSubscriptionAvailable: Boolean? = null
                    var serverKeepAlive: UShort? = null
                    var responseInformation: MqttUtf8String? = null
                    var serverReference: MqttUtf8String? = null
                    var authenticationMethod: MqttUtf8String? = null
                    var authenticationData: ByteArrayWrapper? = null
                    keyValuePairs?.forEach {
                        when (it) {
                            is SessionExpiryInterval -> {
                                if (sessionExpiryIntervalSeconds != null) {
                                    throw ProtocolError("Session Expiry Interval added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477348")
                                }
                                sessionExpiryIntervalSeconds = it.seconds
                            }
                            is ReceiveMaximum -> {
                                if (receiveMaximum != null) {
                                    throw ProtocolError("Receive Maximum added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477349")
                                }
                                if (it.maxQos1Or2ConcurrentMessages == 0.toUShort()) {
                                    throw ProtocolError("Receive Maximum cannot be set to 0 see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477349")
                                }
                                receiveMaximum = it.maxQos1Or2ConcurrentMessages
                            }
                            is MaximumQos -> {
                                if (maximumQos != null) {
                                    throw ProtocolError("")
                                }
                                maximumQos = it.qos
                            }
                            is RetainAvailable -> {
                                if (retainAvailable != null) {
                                    throw ProtocolError("")
                                }
                                retainAvailable = it.serverSupported
                            }
                            is MaximumPacketSize -> {
                                if (maximumPacketSize != null) {
                                    throw ProtocolError("Maximum Packet Size added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477350")
                                }
                                if (it.packetSizeLimitationBytes == 0.toUInt()) {
                                    throw ProtocolError("Maximum Packet Size cannot be set to 0 see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477350")
                                }
                                maximumPacketSize = it.packetSizeLimitationBytes
                            }
                            is AssignedClientIdentifier -> {
                                if (assignedClientIdentifier != null) {
                                    throw ProtocolError("")
                                }
                                assignedClientIdentifier = it.value
                            }
                            is TopicAliasMaximum -> {
                                if (topicAliasMaximum != null) {
                                    throw ProtocolError("Topic Alias Maximum added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477351")
                                }
                                topicAliasMaximum = it.highestValueSupported
                            }
                            is ReasonString -> {
                                if (reasonString != null) {
                                    throw ProtocolError("Request Response Information added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477352")
                                }
                                reasonString = it.diagnosticInfoDontParse
                            }
                            is UserProperty -> userProperty += Pair(it.key, it.value)
                            is WildcardSubscriptionAvailable -> {
                                if (supportsWildcardSubscriptions != null) {
                                    throw ProtocolError("Request Problem Information added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477353")
                                }
                                supportsWildcardSubscriptions = it.serverSupported
                            }
                            is SubscriptionIdentifierAvailable -> {
                                if (subscriptionIdentifiersAvailable != null) {
                                    throw ProtocolError("Request Problem Information added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477353")
                                }
                                subscriptionIdentifiersAvailable = it.serverSupported
                            }
                            is SharedSubscriptionAvailable -> {
                                if (sharedSubscriptionAvailable != null) {
                                    throw ProtocolError("Request Problem Information added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477353")
                                }
                                sharedSubscriptionAvailable = it.serverSupported
                            }
                            is ServerKeepAlive -> {
                                if (serverKeepAlive != null) {
                                    throw ProtocolError("Request Problem Information added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477353")
                                }
                                serverKeepAlive = it.seconds
                            }
                            is ResponseInformation -> {
                                if (responseInformation != null) {
                                    throw ProtocolError("Request Problem Information added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477353")
                                }
                                responseInformation = it.requestResponseInformationInConnack
                            }
                            is ServerReference -> {
                                if (serverReference != null) {
                                    throw ProtocolError("Request Problem Information added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477353")
                                }
                                serverReference = it.otherServer
                            }
                            is AuthenticationMethod -> {
                                if (authenticationMethod != null) {
                                    throw ProtocolError("Authentication Method added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477355")
                                }
                                authenticationMethod = it.value
                            }
                            is AuthenticationData -> {
                                if (authenticationData != null) {
                                    throw ProtocolError("Authentication Data added multiple times see: " +
                                            "https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477356")
                                }
                                authenticationData = it.data
                            }
                            else -> throw MalformedPacketException("Invalid CONNACK property type found in MQTT payload $it")
                        }
                    }
                    val authMethod = authenticationMethod
                    val authData = authenticationData
                    val auth = if (authMethod != null && authData != null) {
                        Authentication(authMethod, authData)
                    } else {
                        null
                    }
                    return Properties(sessionExpiryIntervalSeconds, receiveMaximum ?: UShort.MAX_VALUE,
                            maximumQos ?: QualityOfService.EXACTLY_ONCE, retainAvailable ?: true,
                            maximumPacketSize, assignedClientIdentifier, topicAliasMaximum ?: 0.toUShort(),
                            reasonString, userProperty, supportsWildcardSubscriptions ?: true,
                            subscriptionIdentifiersAvailable ?: true,
                            sharedSubscriptionAvailable ?: true,
                            serverKeepAlive, responseInformation, serverReference, auth)
                }
            }
        }

        fun packet(sendDefaults: Boolean = false): ByteReadPacket {
            return buildPacket {
                writeByte(if (sessionPresent) 0b1 else 0b0)
                writeUByte(connectReason.byte)
                writePacket(properties.packet(sendDefaults))
            }
        }

        companion object {
            fun from(buffer: ByteReadPacket): VariableHeader {
                val sessionPresent = buffer.readByte() == 1.toByte()
                val connectionReasonByte = buffer.readUByte()
                val connectionReason = connackConnectReason[connectionReasonByte]
                if (connectionReason == null) {
                    throw MalformedPacketException("Invalid property type found in MQTT payload $connectionReason")
                }
                val properties = buffer.readProperties()
                val propeties = Properties.from(properties)
                return VariableHeader(sessionPresent, connectionReason, propeties)
            }
        }
    }

    companion object {
        fun from(buffer: ByteReadPacket) = ConnectionAcknowledgment(VariableHeader.from(buffer))
    }
}

val connackConnectReason by lazy {
    mapOf(
            Pair(SUCCESS.byte, SUCCESS),
            Pair(UNSPECIFIED_ERROR.byte, UNSPECIFIED_ERROR),
            Pair(MALFORMED_PACKET.byte, MALFORMED_PACKET),
            Pair(PROTOCOL_ERROR.byte, PROTOCOL_ERROR),
            Pair(IMPLEMENTATION_SPECIFIC_ERROR.byte, IMPLEMENTATION_SPECIFIC_ERROR),
            Pair(UNSUPPORTED_PROTOCOL_VERSION.byte, UNSUPPORTED_PROTOCOL_VERSION),
            Pair(CLIENT_IDENTIFIER_NOT_VALID.byte, CLIENT_IDENTIFIER_NOT_VALID),
            Pair(BAD_USER_NAME_OR_PASSWORD.byte, BAD_USER_NAME_OR_PASSWORD),
            Pair(NOT_AUTHORIZED.byte, NOT_AUTHORIZED),
            Pair(SERVER_UNAVAILABLE.byte, SERVER_UNAVAILABLE),
            Pair(SERVER_BUSY.byte, SERVER_BUSY),
            Pair(BANNED.byte, BANNED),
            Pair(BAD_AUTHENTICATION_METHOD.byte, BAD_AUTHENTICATION_METHOD),
            Pair(TOPIC_NAME_INVALID.byte, TOPIC_NAME_INVALID),
            Pair(PACKET_TOO_LARGE.byte, PACKET_TOO_LARGE),
            Pair(QUOTA_EXCEEDED.byte, QUOTA_EXCEEDED),
            Pair(PAYLOAD_FORMAT_INVALID.byte, PAYLOAD_FORMAT_INVALID),
            Pair(RETAIN_NOT_SUPPORTED.byte, RETAIN_NOT_SUPPORTED),
            Pair(QOS_NOT_SUPPORTED.byte, QOS_NOT_SUPPORTED),
            Pair(USE_ANOTHER_SERVER.byte, USE_ANOTHER_SERVER),
            Pair(SERVER_MOVED.byte, SERVER_MOVED),
            Pair(CONNECTION_RATE_EXCEEDED.byte, CONNECTION_RATE_EXCEEDED)
    )
}
