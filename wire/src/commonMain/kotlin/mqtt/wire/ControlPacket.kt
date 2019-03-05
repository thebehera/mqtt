@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire

import kotlinx.io.core.*
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow.*
import mqtt.wire.control.packet.format.fixed.get
import mqtt.wire.control.packet.format.variable.Property.*
import mqtt.wire.control.packet.format.variable.PropertyKeyValueWrapper
import mqtt.wire.control.packet.format.variable.readProperties
import mqtt.wire.data.*
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.AT_MOST_ONCE

/**
 * The MQTT specification defines fifteen different types of MQTT Control Packet, for example the PublishMessage packet is
 * used to convey Application Messages.
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322
 * @see http://docs.oasis-open.org/mqtt/mqtt/v5.0/cos01/mqtt-v5.0-cos01.html#_Toc514847903
 * @param controlPacketValue Value defined under [MQTT 2.1.2]
 * @param direction Direction of Flow defined under [MQTT 2.1.2]
 */
sealed class ControlPacket(val controlPacketValue: Byte,
                           val direction: DirectionOfFlow,
                           val flags: Byte = 0b0) {

    private val fixedHeader by lazy {
        val packetValue = controlPacketValue
        val packetValueUInt = packetValue.toUInt()
        val packetValueShifted = packetValueUInt.shl(4)
        val localFlagsByte = flags.toUInt()
        val byte1 = (packetValueShifted.toByte().toUInt() + localFlagsByte).toUByte()
        val byte2 = VariableByteInteger(remainingLength)
        buildPacket {
            writeUByte(byte1)
            writePacket(byte2.encodedValue())
        }.readBytes()
    }

    open val variableHeaderPacket: ByteArray? = null
    open val payloadPacket: ByteArray? = null
    private val remainingLength by lazy {
        val variableHeaderSize = variableHeaderPacket?.size ?: 0
        val payloadSize = payloadPacket?.size ?: 0
        (variableHeaderSize + payloadSize).toUInt()
    }

    val serialize by lazy {
        buildPacket {
            writeFully(fixedHeader)
            val variableHeaderPacket = variableHeaderPacket
            if (variableHeaderPacket != null) {
                writeFully(variableHeaderPacket)
            }
            val payloadPacket = payloadPacket
            if (payloadPacket != null) {
                writeFully(payloadPacket)
            }
        }.readBytes()
    }
    companion object {
        fun from(buffer: ByteReadPacket): ControlPacket {
            val byte1AsUInt = buffer.readUByte().toUInt()
            val packetValue = byte1AsUInt.shr(4).toInt()
            buffer.decodeVariableByteInteger() // remaining Length
            return when (packetValue) {
                0x00 -> Reserved
                0x01 -> ConnectionRequest.from(buffer)
                else -> throw MalformedPacketException("Invalid MQTT Control Packet Type: $packetValue Should be in range between 0 and 15 inclusive")
            }
        }
    }
}

object Reserved : ControlPacket(0, FORBIDDEN)

data class ConnectionRequest(val variableHeader: VariableHeader = VariableHeader(), val payload: Payload = Payload())
    : ControlPacket(1, CLIENT_TO_SERVER) {
    override val variableHeaderPacket: ByteArray get() = variableHeader.packet
    override val payloadPacket: ByteArray? get() = payload.packet
    data class VariableHeader(
            val protocolName: MqttUtf8String = MqttUtf8String("MQTT"),
            val protocolVersion: UByte = 5.toUByte(),
            val hasUserName: Boolean = false,
            val hasPassword: Boolean = false,
            val willRetain: Boolean = false,
            val willQos: QualityOfService = AT_LEAST_ONCE,
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

            data class Authentication(val method: MqttUtf8String, val data: ByteReadPacket) {
                val packet by lazy {
                    buildPacket {
                        writePacket(AUTHENTICATION_METHOD.identifier.encodedValue())
                        writeMqttUtf8String(method)
                        writePacket(AUTHENTICATION_DATA.identifier.encodedValue())
                        writePacket(data)
                    }
                }
            }

            init {
                if (maximumPacketSize != null || maximumPacketSize == 0.toUInt()) {
                    throw ProtocolError("Maximum Packet Size cannot be set to zero")
                }
            }

            val packet by lazy {
                val propertiesPacket = buildPacket {
                    if (sessionExpiryIntervalSeconds != null) {
                        writePacket(SESSION_EXPIRY_INTERVAL.identifier.encodedValue())
                        writeUInt(sessionExpiryIntervalSeconds)
                    }
                    if (receiveMaximum != null) {
                        writePacket(RECEIVE_MAXIMUM.identifier.encodedValue())
                        writeUShort(receiveMaximum)
                    }
                    if (maximumPacketSize != null) {
                        writePacket(MAXIMUM_PACKET_SIZE.identifier.encodedValue())
                        writeUInt(maximumPacketSize)
                    }
                    if (topicAliasMaximum != null) {
                        writePacket(TOPIC_ALIAS_MAXIMUM.identifier.encodedValue())
                        writeUShort(topicAliasMaximum)
                    }
                    if (requestResponseInformation != null) {
                        writePacket(REQUEST_RESPONSE_INFORMATION.identifier.encodedValue())
                        writeUByte(if (requestResponseInformation) 1.toUByte() else 0.toUByte())
                    }
                    if (requestProblemInformation != null) {
                        writePacket(REQUEST_PROBLEM_INFORMATION.identifier.encodedValue())
                        writeUByte(if (requestProblemInformation) 1.toUByte() else 0.toUByte())
                    }
                    if (userProperty != null && userProperty.isNotEmpty()) {
                        for (keyValueProperty in userProperty) {
                            val key = keyValueProperty.first
                            val value = keyValueProperty.second
                            writePacket(USER_PROPERTY.identifier.encodedValue())
                            writeMqttUtf8String(key)
                            writeMqttUtf8String(value)
                        }
                    }
                    if (authentication != null) {
                        writePacket(authentication.packet)
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
                fun from(keyValuePairs :Collection<PropertyKeyValueWrapper>): Properties {
                    var sessionExpiryIntervalSeconds: UInt? = null
                    var receiveMaximum: UShort? = null
                    var maximumPacketSize: UInt? = null
                    var topicAliasMaximum: UShort? = null
                    var requestResponseInformation: Boolean? = null
                    var requestProblemInformation: Boolean? = null
                    var userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = mutableListOf()
                    var authenticationMethod :String? = null
                    var authenticationData :ByteArray? = null
                    keyValuePairs.forEach {
                        when (it.property) {
                            SESSION_EXPIRY_INTERVAL -> sessionExpiryIntervalSeconds = it.number
                            RECEIVE_MAXIMUM -> receiveMaximum = it.number?.toUShort()
                            MAXIMUM_PACKET_SIZE -> maximumPacketSize = it.number?.toUInt()
                            TOPIC_ALIAS_MAXIMUM -> topicAliasMaximum = it.number?.toUShort()
                            REQUEST_RESPONSE_INFORMATION -> requestResponseInformation = it.number == 1.toUInt()
                            REQUEST_PROBLEM_INFORMATION -> requestProblemInformation = it.number == 1.toUInt()
                            USER_PROPERTY -> {
                                val key = it.key
                                val value = it.value
                                if (key != null && value != null) {
                                    userProperty += Pair(MqttUtf8String(key), MqttUtf8String(value))
                                }
                            }
                            AUTHENTICATION_METHOD -> authenticationMethod = it.value
                            AUTHENTICATION_DATA -> authenticationData = it.binary
                            else -> throw MalformedPacketException("Invalid property type found in MQTT payload ${it.property}")
                        }
                    }
                    val authMethod = authenticationMethod
                    val authData = authenticationData
                    val auth = if (authMethod != null && authData != null) {
                        Authentication(MqttUtf8String(authMethod), buildPacket { writeFully(authData) })
                    } else { null }
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
            fun from(buffer: ByteReadPacket) :VariableHeader {
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
                val correlationData: ByteReadPacket? = null,
                val userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>>? = null) {
            val packet by lazy {
                buildPacket {
                    if (willDelayIntervalSeconds != null) {
                        writePacket(WILL_DELAY_INTERVAL.identifier.encodedValue())
                        writeUInt(willDelayIntervalSeconds)
                    }
                    if (payloadFormatIndicator != null) {
                        writePacket(PAYLOAD_FORMAT_INDICATOR.identifier.encodedValue())
                        writeUByte(if (payloadFormatIndicator) 0x01.toUByte() else 0x00.toUByte())
                    }
                    if (messageExpiryIntervalSeconds != null) {
                        writePacket(MESSAGE_EXPIRY_INTERVAL.identifier.encodedValue())
                        writeUInt(messageExpiryIntervalSeconds)
                    }
                    if (contentType != null) {
                        writePacket(CONTENT_TYPE.identifier.encodedValue())
                        writeMqttUtf8String(contentType)
                    }
                    if (responseTopic != null) {
                        writePacket(RESPONSE_TOPIC.identifier.encodedValue())
                        writeMqttUtf8String(responseTopic)
                    }
                    if (correlationData != null) {
                        writePacket(CORRELATION_DATA.identifier.encodedValue())
                        writePacket(correlationData)
                    }
                    if (userProperty != null && userProperty.isNotEmpty()) {
                        for (keyValueProperty in userProperty) {
                            val key = keyValueProperty.first
                            val value = keyValueProperty.second
                            writePacket(USER_PROPERTY.identifier.encodedValue())
                            writeMqttUtf8String(key)
                            writeMqttUtf8String(value)
                        }
                    }
                }.readBytes()
            }
            companion object {
                fun from(buffer: ByteReadPacket) :WillProperties {
                    val properties = buffer.readProperties()
                    var willDelayIntervalSeconds: UInt? = null
                    var payloadFormatIndicator: Boolean? = null
                    var messageExpiryIntervalSeconds: UInt? = null
                    var contentType: MqttUtf8String? = null
                    var responseTopic: MqttUtf8String? = null
                    var correlationData: ByteReadPacket? = null
                    var userProperty: Collection<Pair<MqttUtf8String, MqttUtf8String>> = mutableListOf()
                    properties.forEach {
                        when (it.property) {
                            WILL_DELAY_INTERVAL -> willDelayIntervalSeconds = it.number
                            PAYLOAD_FORMAT_INDICATOR -> payloadFormatIndicator = it.number == 1.toUInt()
                            MESSAGE_EXPIRY_INTERVAL -> messageExpiryIntervalSeconds = it.number
                            CONTENT_TYPE -> contentType = MqttUtf8String(it.value!!)
                            RESPONSE_TOPIC -> responseTopic = MqttUtf8String(it.value!!)
                            CORRELATION_DATA -> correlationData = buildPacket { writeFully(it.binary!!) }
                            USER_PROPERTY -> {
                                val key = it.key
                                val value = it.value
                                if (key != null && value != null) {
                                    userProperty += Pair(MqttUtf8String(key), MqttUtf8String(value))
                                }
                            }
                            else -> throw MalformedPacketException("Invalid property type found in MQTT payload ${it.property}")
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
            fun from(buffer: ByteReadPacket, variableHeader: VariableHeader) :Payload {
                val clientId = buffer.readMqttUtf8String()
                val willProperties = if (variableHeader.willFlag) { WillProperties.from(buffer) } else { null }
                val willTopic = if (variableHeader.willFlag) { buffer.readMqttUtf8String() } else { null }
                val willPayload = if (variableHeader.willFlag) { buildPacket { writeFully(buffer.readMqttBinary()) } } else { null }
                val username = if (variableHeader.hasUserName) { buffer.readMqttUtf8String() } else { null }
                val password = if (variableHeader.hasPassword) { buffer.readMqttUtf8String() } else { null }
                return Payload(clientId, willProperties, willTopic, willPayload, username, password)
            }
        }
    }

    companion object {
        fun from(buffer: ByteReadPacket) :ConnectionRequest {
            val variableHeader = VariableHeader.from(buffer)
            val payload = Payload.from(buffer, variableHeader)
            return ConnectionRequest(variableHeader, payload)
        }
    }
}

object ConnectionAcknowledgment : ControlPacket(2, SERVER_TO_CLIENT)

/**
 * Creates an MQTT PUBLISH
 * @param dup Duplicate delivery of a PublishMessage packet
 * @param qos PublishMessage Quality of Service
 * @param retain PublishMessage retained message flag
 * @param packetIdentifier Packet Identifier for QOS > 0 packet identifier
 */
data class PublishMessage(val dup: Boolean = false,
                          val qos: QualityOfService = AT_MOST_ONCE,
                          val retain: Boolean = false,
                          val packetIdentifier: UShort? = null)
    : ControlPacket(3, BIDIRECTIONAL, PublishMessage.flags(dup, qos, retain)) {
    init {
        if (qos == AT_MOST_ONCE && packetIdentifier != null) {
            throw IllegalArgumentException("Cannot allocate a publish message with a QoS of 0 with a packet identifier")
        } else if (qos.isGreaterThan(AT_MOST_ONCE) && packetIdentifier == null) {
            throw IllegalArgumentException("Cannot allocate a publish message with a QoS >0 and no packet identifier")
        }
    }
    companion object {
        fun flags(dup: Boolean, qos: QualityOfService, retain: Boolean): Byte {
            val dupInt = if (dup) 0b1000 else 0b0
            val qosInt = qos.integerValue.toInt().shl(1)
            val retainInt = if (retain) 0b1 else 0b0
            return (dupInt or qosInt or retainInt).toByte()
        }
    }
}

data class PublishAcknowledgment(val packetIdentifier: UShort) : ControlPacket(4, BIDIRECTIONAL)

data class PublishReceived(val packetIdentifier: UShort) : ControlPacket(5, BIDIRECTIONAL)

data class PublishRelease(val packetIdentifier: UShort) : ControlPacket(6, BIDIRECTIONAL, 0b10)

data class PublishComplete(val packetIdentifier: UShort) : ControlPacket(7, BIDIRECTIONAL)

data class SubscribeRequest(val packetIdentifier: UShort) : ControlPacket(8, CLIENT_TO_SERVER, 0b10)

data class SubscribeAcknowledgment(val packetIdentifier: UShort) : ControlPacket(9, SERVER_TO_CLIENT)

data class UnsubscribeRequest(val packetIdentifier: UShort) : ControlPacket(10, CLIENT_TO_SERVER, 0b10)

data class UnsubscribeAcknowledgment(val packetIdentifier: UShort) : ControlPacket(11, SERVER_TO_CLIENT)

object PingRequest : ControlPacket(12, CLIENT_TO_SERVER)

object PingResponse : ControlPacket(13, SERVER_TO_CLIENT)

object DisconnectNotification : ControlPacket(14, BIDIRECTIONAL)

object AuthenticationExchange : ControlPacket(15, BIDIRECTIONAL)
