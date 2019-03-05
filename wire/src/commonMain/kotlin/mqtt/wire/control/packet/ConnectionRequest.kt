@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import kotlinx.io.core.*
import mqtt.wire.MalformedPacketException
import mqtt.wire.ProtocolError
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.control.packet.format.fixed.get
import mqtt.wire.control.packet.format.variable.property.*
import mqtt.wire.data.*

data class ConnectionRequest(val variableHeader: VariableHeader = VariableHeader(), val payload: Payload = Payload())
    : ControlPacket(1, DirectionOfFlow.CLIENT_TO_SERVER) {
    override val variableHeaderPacket: ByteArray get() = variableHeader.packet
    override val payloadPacket: ByteArray? get() = payload.packet

    data class VariableHeader(
            val protocolName: MqttUtf8String = MqttUtf8String("MQTT"),
            val protocolVersion: UByte = 5.toUByte(),
            val hasUserName: Boolean = false,
            val hasPassword: Boolean = false,
            val willRetain: Boolean = false,
            val willQos: QualityOfService = QualityOfService.AT_LEAST_ONCE,
            val willFlag: Boolean = false,
            val cleanStart: Boolean = false,
            val keepAliveSeconds: UShort = UShort.MAX_VALUE,
            val properties: Properties = Properties()) {
        data class Properties(
                val sessionExpiryIntervalSeconds: UInt? = null,
                val receiveMaximum: UShort? = null,
                val maximumPacketSize: UInt? = null,
                val topicAliasMaximum: UShort? = null,
                val requestResponseInformation: Boolean? = null,
                val requestProblemInformation: Boolean? = null,
                val userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>>? = null,
                val authentication: Authentication? = null) {

            data class Authentication(val method: MqttUtf8String, val data: ByteArrayWrapper)

            init {
                if (maximumPacketSize != null || maximumPacketSize == 0.toUInt()) {
                    throw ProtocolError("Maximum Packet Size cannot be set to zero")
                }
            }

            val packet by lazy {
                val propertiesPacket = buildPacket {
                    if (sessionExpiryIntervalSeconds != null) {
                        SessionExpiryInterval(sessionExpiryIntervalSeconds).write(this)
                    }
                    if (receiveMaximum != null) {
                        ReceiveMaximum(receiveMaximum).write(this)
                    }
                    if (maximumPacketSize != null) {
                        MaximumPacketSize(maximumPacketSize).write(this)
                    }
                    if (topicAliasMaximum != null) {
                        TopicAliasMaximum(topicAliasMaximum).write(this)
                    }
                    if (requestResponseInformation != null) {
                        RequestResponseInformation(requestResponseInformation).write(this)
                    }
                    if (requestProblemInformation != null) {
                        RequestProblemInformation(requestProblemInformation).write(this)
                    }
                    if (userProperty != null && userProperty.isNotEmpty()) {
                        for (keyValueProperty in userProperty) {
                            val key = keyValueProperty.first
                            val value = keyValueProperty.second
                            UserProperty(key, value).write(this)
                        }
                    }
                    if (authentication != null) {
                        AuthenticationMethod(authentication.method).write(this)
                        AuthenticationData(authentication.data).write(this)
                    }
                }.readBytes()
                val propertyLength = propertiesPacket.size
                val result = buildPacket {
                    writePacket(VariableByteInteger(propertyLength.toUInt()).encodedValue())
                    writeFully(propertiesPacket)
                }.readBytes()

                result
            }

            companion object {
                fun from(keyValuePairs: Collection<Property>): Properties {
                    var sessionExpiryIntervalSeconds: UInt? = null
                    var receiveMaximum: UShort? = null
                    var maximumPacketSize: UInt? = null
                    var topicAliasMaximum: UShort? = null
                    var requestResponseInformation: Boolean? = null
                    var requestProblemInformation: Boolean? = null
                    var userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = mutableListOf()
                    var authenticationMethod: MqttUtf8String? = null
                    var authenticationData: ByteArrayWrapper? = null
                    keyValuePairs.forEach {
                        when (it) {
                            is SessionExpiryInterval -> sessionExpiryIntervalSeconds = it.seconds
                            is ReceiveMaximum -> receiveMaximum = it.maxQos1Or2ConcurrentMessages
                            is MaximumPacketSize -> maximumPacketSize = it.packetSizeLimitationBytes
                            is TopicAliasMaximum -> topicAliasMaximum = it.highestValueSupported
                            is RequestResponseInformation -> requestResponseInformation = it.requestServerToReturnInfoInConnack
                            is RequestProblemInformation -> requestProblemInformation = it.reasonStringOrUserPropertiesAreSentInFailures
                            is UserProperty -> userProperty += Pair(it.key, it.value)
                            is AuthenticationMethod -> authenticationMethod = it.value
                            is AuthenticationData -> authenticationData = it.data
                            else -> throw MalformedPacketException("Invalid CONNECT property type found in MQTT payload $it")
                        }
                    }
                    val authMethod = authenticationMethod
                    val authData = authenticationData
                    val auth = if (authMethod != null && authData != null) {
                        Authentication(authMethod, authData)
                    } else {
                        null
                    }
                    return Properties(sessionExpiryIntervalSeconds, receiveMaximum, maximumPacketSize,
                            topicAliasMaximum, requestResponseInformation, requestProblemInformation, userProperty, auth)
                }
            }
        }

        /**
         * The Variable Header for the CONNECT Packet contains the following fields in this order: Protocol Name,
         * Protocol Level, Connect Flags, Keep Alive, and Properties
         */
        val packet by lazy {
            val usernameFlag = if (hasUserName) 0b10000000 else 0
            val passwordFlag = if (hasPassword) 0b1000000 else 0
            val wRetain = if (willRetain) 0b100000 else 0
            val qos = willQos.integerValue.toInt().shl(3)
            val wFlag = if (willFlag) 0b100 else 0
            val cleanStart = if (cleanStart) 0b10 else 0
            val flags = (usernameFlag or passwordFlag or wRetain or qos or wFlag or cleanStart).toByte()
            buildPacket {
                writeMqttUtf8String(protocolName)
                writeUByte(protocolVersion)
                writeByte(flags)
                writeUShort(keepAliveSeconds)
                writeFully(properties.packet)
            }.readBytes()
        }

        companion object {
            fun from(buffer: ByteReadPacket): VariableHeader {
                val protocolName = buffer.readMqttUtf8String()
                val protocolVersion = buffer.readUByte()
                val connectFlags = buffer.readUByte()
                val hasUsername = connectFlags.get(7)
                val hasPassword = connectFlags.get(6)
                val willRetain = connectFlags.get(5)
                val willQosBit2 = connectFlags.get(4)
                val willQosBit1 = connectFlags.get(3)
                val willQos = QualityOfService.fromBooleans(willQosBit2, willQosBit1)
                val willFlag = connectFlags.get(2)
                val cleanStart = connectFlags.get(1)
                val reserved = connectFlags.get(0)
                if (reserved) {
                    throw MalformedPacketException(
                            "Reserved flag in Connect Variable Header packet is set incorrectly to 1")
                }
                val keepAliveSeconds = buffer.readUShort()
                val propertiesRaw = buffer.readProperties()
                val properties = Properties.from(propertiesRaw)
                return VariableHeader(protocolName, protocolVersion, hasUsername, hasPassword, willRetain, willQos,
                        willFlag, cleanStart, keepAliveSeconds, properties)
            }
        }
    }

    data class Payload(
            val clientId: MqttUtf8String = MqttUtf8String(""),
            val willProperties: WillProperties? = null,
            val willTopic: MqttUtf8String? = null,
            val willPayload: ByteReadPacket? = null,
            val userName: MqttUtf8String? = null,
            val password: MqttUtf8String? = null
    ) {
        data class WillProperties(
                val willDelayIntervalSeconds: UInt? = null,
                val payloadFormatIndicator: Boolean? = null,
                val messageExpiryIntervalSeconds: UInt? = null,
                val contentType: MqttUtf8String? = null,
                val responseTopic: MqttUtf8String? = null,
                val correlationData: ByteArrayWrapper? = null,
                val userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>>? = null) {
            val packet by lazy {
                buildPacket {
                    if (willDelayIntervalSeconds != null) {
                        WillDelayInterval(willDelayIntervalSeconds).write(this)
                    }
                    if (payloadFormatIndicator != null) {
                        PayloadFormatIndicator(payloadFormatIndicator).write(this)
                    }
                    if (messageExpiryIntervalSeconds != null) {
                        MessageExpiryInterval(messageExpiryIntervalSeconds).write(this)
                    }
                    if (contentType != null) {
                        ContentType(contentType).write(this)
                    }
                    if (responseTopic != null) {
                        ResponseTopic(responseTopic).write(this)
                    }
                    if (correlationData != null) {
                        CorrelationData(correlationData).write(this)
                    }
                    if (userProperty != null && userProperty.isNotEmpty()) {
                        for (keyValueProperty in userProperty) {
                            val key = keyValueProperty.first
                            val value = keyValueProperty.second
                            UserProperty(key, value).write(this)
                        }
                    }
                }.readBytes()
            }

            companion object {
                fun from(buffer: ByteReadPacket): WillProperties {
                    val properties = buffer.readProperties()
                    var willDelayIntervalSeconds: UInt? = null
                    var payloadFormatIndicator: Boolean? = null
                    var messageExpiryIntervalSeconds: UInt? = null
                    var contentType: MqttUtf8String? = null
                    var responseTopic: MqttUtf8String? = null
                    var correlationData: ByteArrayWrapper? = null
                    var userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = mutableListOf()
                    properties.forEach {
                        when (it) {
                            is WillDelayInterval -> willDelayIntervalSeconds = it.seconds
                            is PayloadFormatIndicator -> payloadFormatIndicator = it.willMessageIsUtf8
                            is MessageExpiryInterval -> messageExpiryIntervalSeconds = it.seconds
                            is ContentType -> contentType = it.value
                            is ResponseTopic -> responseTopic = it.value
                            is CorrelationData -> correlationData = it.data
                            is UserProperty -> {
                                val key = it.key
                                val value = it.value
                                userProperty += Pair(key, value)
                            }
                            else -> throw MalformedPacketException("Invalid property type found in MQTT payload $it")
                        }
                    }
                    return WillProperties(willDelayIntervalSeconds, payloadFormatIndicator,
                            messageExpiryIntervalSeconds, contentType, responseTopic, correlationData, userProperty)
                }
            }
        }

        val packet by lazy {
            buildPacket {
                writeMqttUtf8String(clientId)
                if (willProperties != null) {
                    writeFully(willProperties.packet)
                }
                if (willTopic != null) {
                    writeMqttUtf8String(willTopic)
                }
                if (willPayload != null) {
                    val payload = willPayload.readBytes()
                    writeInt(payload.size)
                    writeFully(payload)
                }
                if (userName != null) {
                    writeMqttUtf8String(userName)
                }
                if (password != null) {
                    writeMqttUtf8String(password)
                }
            }.readBytes()
        }

        companion object {
            fun from(buffer: ByteReadPacket, variableHeader: VariableHeader): Payload {
                val clientId = buffer.readMqttUtf8String()
                val willProperties = if (variableHeader.willFlag) {
                    WillProperties.from(buffer)
                } else {
                    null
                }
                val willTopic = if (variableHeader.willFlag) {
                    buffer.readMqttUtf8String()
                } else {
                    null
                }
                val willPayload = if (variableHeader.willFlag) {
                    buildPacket { writeFully(buffer.readMqttBinary()) }
                } else {
                    null
                }
                val username = if (variableHeader.hasUserName) {
                    buffer.readMqttUtf8String()
                } else {
                    null
                }
                val password = if (variableHeader.hasPassword) {
                    buffer.readMqttUtf8String()
                } else {
                    null
                }
                return Payload(clientId, willProperties, willTopic, willPayload, username, password)
            }
        }
    }

    companion object {
        fun from(buffer: ByteReadPacket): ConnectionRequest {
            val variableHeader = VariableHeader.from(buffer)
            val payload = Payload.from(buffer, variableHeader)
            return ConnectionRequest(variableHeader, payload)
        }
    }
}
