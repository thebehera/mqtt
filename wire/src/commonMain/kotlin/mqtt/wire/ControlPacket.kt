@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire

import kotlinx.io.core.*
import mqtt.wire.control.packet.format.fixed.*
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow.*
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.*
import mqtt.wire.data.toMqttUtf8Encoded
import mqtt.wire.data.validateMqttUTF8String

/**
 * The MQTT specification defines fifteen different types of MQTT Control Packet, for example the PublishMessage packet is
 * used to convey Application Messages.
 * @see https://docs.oasis-open.org/mqtt/mqtt/v5.0/cos02/mqtt-v5.0-cos02.html#_Toc1477322
 * @see http://docs.oasis-open.org/mqtt/mqtt/v5.0/cos01/mqtt-v5.0-cos01.html#_Toc514847903
 * @param unsigned4BitValue Value defined under [MQTT 2.1.2]
 * @param direction Direction of Flow defined under [MQTT 2.1.2]
 */
sealed class ControlPacket(val unsigned4BitValue: Byte,
                           val direction: DirectionOfFlow,
                           val flagBits: FlagBits = emptyFlagBits) {

    val fixedHeader by lazy {
        val packetValue = unsigned4BitValue
        val packetValueInt = packetValue.toInt()
        val packetValueShifted = packetValueInt.shl(4)
        val localFlagsByte = flagBits.toByte()
        val byte1 = (packetValueShifted.toByte() + localFlagsByte).toByte()
        val byte2 = remainingLengthVariableByteInteger(remainingLength)
        byteArrayOf(byte1, *byte2)
    }

    open val variableHeader = byteArrayOf()
    open val payload = byteArrayOf()
    val remainingLength get() = variableHeader.size + payload.size

}

object Reserved : ControlPacket(0, FORBIDDEN)

/**
 * After a Network Connection is established by a Client to a Server, the first packet sent from the Client to the
 * Server MUST be a CONNECT packet [MQTT-3.1.0-1].
 * A Client can only send the CONNECT packet once over a Network Connection. The Server MUST process a second CONNECT
 * packet sent from a Client as a Protocol Error and close the Network Connection [MQTT-3.1.0-2]. Refer to section 4.13
 * for information about handling errors.
 * The Payload contains one or more encoded fields. They specify a unique Client identifier for the Client, a Will
 * Topic, Will Payload, User Name and Password. All but the Client identifier can be omitted and their presence is
 * determined based on flags in the Variable Header.
 * @param protocolName
 * @param protocolVersion
 * @param userName
 * @param password
 * @param willRetain
 * @param willQos
 * @param willFlag If the Will Flag is set to 1 this indicates that a Will Message MUST be stored on the Server and
 * associated with the Session [MQTT-3.1.2-7]. The Will Message consists of the Will Properties, Will Topic, and Will
 * Payload fields in the CONNECT Payload. The Will Message MUST be published after the Network Connection is
 * subsequently closed and either the Will Delay Interval has elapsed or the Session ends, unless the Will Message has
 * been deleted by the Server on receipt of a DISCONNECT packet with Reason Code 0x00 (Normal disconnection) or a new
 * Network Connection for the ClientID is opened before the Will Delay Interval has elapsed [MQTT-3.1.2-8]. <br/>
 * Situations in which the Will Message is published include, but are not limited to:<br/>
 *          An I/O error or network failure detected by the Server.<br/>
 *          The Client fails to communicate within the Keep Alive time.<br/>
 *          The Client closes the Network Connection without first sending a DISCONNECT packet with a Reason Code 0x00
 *          (Normal disconnection).<br/>
 *          The Server closes the Network Connection without first receiving a DISCONNECT packet with a Reason Code 0x00
 *          (Normal disconnection).<br/>
 * If the Will Flag is set to 1, the Will Properties, Will Topic, and Will Payload fields MUST be present in the Payload
 * [MQTT-3.1.2-9]. The Will Message MUST be removed from the stored Session State in the Server once it has been
 * published or the Server has received a DISCONNECT packet with a Reason Code of 0x00 (Normal disconnection) from the
 * Client [MQTT-3.1.2-10].
 * The Server SHOULD publish Will Messages promptly after the Network Connection is closed and the Will Delay Interval
 * has passed, or when the Session ends, whichever occurs first. In the case of a Server shutdown or failure, the Server
 * MAY defer publication of Will Messages until a subsequent restart. If this happens, there might be a delay between
 * the time the Server experienced failure and when the Will Message is published.
 *
 * @param cleanStart This bit specifies whether the Connection starts a new Session or is a continuation of an existing
 * Session. Refer to section 4.1 for a definition of the Session State. If a CONNECT packet is received with Clean Start
 * is set to 1, the Client and Server MUST discard any existing Session and start a new Session [MQTT-3.1.2-4].
 * Consequently, the Session Present flag in CONNACK is always set to 0 if Clean Start is set to 1.  If a CONNECT packet
 * is received with Clean Start set to 0 and there is a Session associated with the Client Identifier, the Server MUST
 * resume communications with the Client based on state from the existing Session [MQTT-3.1.2-5]. If a CONNECT packet is
 * received with Clean Start set to 0 and there is no Session associated with the Client Identifier, the Server MUST
 * create a new Session [MQTT-3.1.2-6].
 */
data class ConnectionRequest(
        val protocolName: String = "MQTT",
        val protocolVersion: UByte = 5.toUByte(),
        val userName: String?,
        val password: String?,
        val willRetain: Boolean,
        val willQos: QualityOfService,
        val willFlag: Boolean,
        val cleanStart: Boolean,
        val keepAliveSeconds: UShort,
        val sessionExpiryIntervalSeconds: UInt?,
        val receiveMaximum: UShort?,
        val maximumPacketSize: UInt?,
        val topicAliasMaximum: UByte?,


        ) : ControlPacket(1, CLIENT_TO_SERVER) {

//    init {
//        if (maximumPacketSize != null || maximumPacketSize == 0.toUInt()) {
//            throw ProtocolError("Maximum Packet Size value to be set to zero") // Think about errors vs
//        }
//    }

    /**
     * The Variable Header for the CONNECT Packet contains the following fields in this order: Protocol Name,
     * Protocol Level, Connect Flags, Keep Alive, and Properties
     */
    override val variableHeader: ByteArray by lazy {
        if (protocolName.validateMqttUTF8String()) {
            throw IllegalArgumentException("Invalid Protocol Name")
        }
        val qosBitInfo = willQos.toBitInformation()
        val connectFlags = booleanArrayOf(userName != null, password != null, willRetain, qosBitInfo.first,
                qosBitInfo.second, willFlag, cleanStart, false).toByte()

        val connectProperties = buildPacket {
            if (sessionExpiryIntervalSeconds != null) {
                writeUInt(sessionExpiryIntervalSeconds)
            }
        }
        val packet = buildPacket {
            writeFully(protocolName.toMqttUtf8Encoded())
            writeUByte(protocolVersion)
            writeByte(connectFlags)
            writeUShort(keepAliveSeconds)


        }
        packet.readBytes()
    }
}

object ConnectionAcknowledgment : ControlPacket(2, SERVER_TO_CLIENT)

/**
 * Creates an MQTT PUBLISH
 * @param packetIdentifier Packet Identifier for QOS > 0 packet identifier
 * @param dup Duplicate delivery of a PublishMessage packet
 * @param qos PublishMessage Quality of Service
 * @param retain PublishMessage retained message flag
 */
sealed class PublishMessage(open val packetIdentifier: UShort?,
                            open val dup: Boolean,
                            val qos: QualityOfService,
                            open val retain: Boolean)
    : ControlPacket(unsigned4BitValue, BIDIRECTIONAL, FlagBits(dup, qos, retain)) {
    companion object {
        const val unsigned4BitValue = 3.toByte()
    }
}

data class PublishMessageQos0(override val dup: Boolean = false, override val retain: Boolean = false)
    : PublishMessage(null, dup, AT_MOST_ONCE, retain)

data class PublishMessageQos1(override val packetIdentifier: UShort, override val dup: Boolean = false, override val retain: Boolean = false)
    : PublishMessage(packetIdentifier, dup, AT_LEAST_ONCE, retain)

data class PublishMessageQos2(override val packetIdentifier: UShort, override val dup: Boolean = false, override val retain: Boolean = false)
    : PublishMessage(packetIdentifier, dup, EXACTLY_ONCE, retain)

data class PublishAcknowledgment(val packetIdentifier: UShort) : ControlPacket(4, BIDIRECTIONAL)

data class PublishReceived(val packetIdentifier: UShort) : ControlPacket(5, BIDIRECTIONAL)

data class PublishRelease(val packetIdentifier: UShort) : ControlPacket(6, BIDIRECTIONAL, bit1TrueFlagBits)

data class PublishComplete(val packetIdentifier: UShort) : ControlPacket(7, BIDIRECTIONAL)

data class SubscribeRequest(val packetIdentifier: UShort) : ControlPacket(8, CLIENT_TO_SERVER, bit1TrueFlagBits)

data class SubscribeAcknowledgment(val packetIdentifier: UShort) : ControlPacket(9, SERVER_TO_CLIENT)

data class UnsubscribeRequest(val packetIdentifier: UShort) : ControlPacket(10, CLIENT_TO_SERVER, bit1TrueFlagBits)

data class UnsubscribeAcknowledgment(val packetIdentifier: UShort) : ControlPacket(11, SERVER_TO_CLIENT)

object PingRequest : ControlPacket(12, CLIENT_TO_SERVER)

object PingResponse : ControlPacket(13, SERVER_TO_CLIENT)

object DisconnectNotification : ControlPacket(14, BIDIRECTIONAL)

object AuthenticationExchange : ControlPacket(15, BIDIRECTIONAL)
