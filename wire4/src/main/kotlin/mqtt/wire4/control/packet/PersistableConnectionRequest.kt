package mqtt.wire4.control.packet

import androidx.room.Embedded
import androidx.room.Ignore
import mqtt.IgnoredOnParcel
import mqtt.Parcelize
import mqtt.wire.MqttWarning
import mqtt.wire.control.packet.IConnectionRequest
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow
import mqtt.wire.data.ByteArrayWrapper
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import mqtt.wire4.control.packet.ConnectionRequest.Payload
import mqtt.wire4.control.packet.ConnectionRequest.VariableHeader

/**
 * Android RoomDB version of [ConnectionRequest]
 */
@Parcelize
data class PersistableConnectionRequest(
    @Embedded val variableHeader: VariableHeader = VariableHeader(),
    @Embedded val payload: Payload = Payload()
) : ControlPacketV4(1, DirectionOfFlow.CLIENT_TO_SERVER), IConnectionRequest {
    @Ignore
    @IgnoredOnParcel
    override val username = payload.userName?.getValueOrThrow()
    @Ignore
    @IgnoredOnParcel
    override val clientIdentifier: String = payload.clientId.getValueOrThrow()
    @Ignore
    @IgnoredOnParcel
    override val protocolName = variableHeader.protocolName.getValueOrThrow()
    @Ignore
    @IgnoredOnParcel
    override val protocolVersion = variableHeader.protocolLevel.toInt()

    constructor(
        clientId: String,
        username: String? = null,
        password: String? = null,
        willRetain: Boolean = false,
        willQos: QualityOfService = QualityOfService.AT_MOST_ONCE,
        willTopic: String? = null,
        willPayload: ByteArrayWrapper? = null,
        cleanSession: Boolean = false,
        keepAliveSeconds: UShort = UShort.MAX_VALUE
    ) :
            this(
                VariableHeader(
                    hasUserName = username != null,
                    hasPassword = password != null,
                    willFlag = willTopic != null && willPayload != null,
                    willQos = willQos,
                    willRetain = willRetain,
                    cleanSession = cleanSession,
                    keepAliveSeconds = keepAliveSeconds.toInt()
                ),
                Payload(
                    clientId = MqttUtf8String(clientId),
                    willTopic = if (willTopic != null && willPayload != null) MqttUtf8String(willTopic) else null,
                    willPayload = if (willTopic != null && willPayload != null) willPayload else null,
                    userName = if (username != null) MqttUtf8String(username) else null,
                    password = if (password != null) MqttUtf8String(password) else null
                )
            )


    @Ignore
    @IgnoredOnParcel
    override val keepAliveTimeoutSeconds: UShort = variableHeader.keepAliveSeconds.toUShort()
    @Ignore
    @IgnoredOnParcel
    override val variableHeaderPacket = variableHeader.packet()

    @Ignore
    override fun payloadPacket(sendDefaults: Boolean) = payload.packet()

    @Ignore
    @IgnoredOnParcel
    override val cleanStart: Boolean = variableHeader.cleanSession

    @Ignore
    override fun copy(): IConnectionRequest = copy(variableHeader = variableHeader, payload = payload)

    override fun validateOrGetWarning(): MqttWarning? {
        if (variableHeader.willFlag &&
            (payload.willPayload == null || payload.willTopic == null)
        ) {
            return MqttWarning(
                "[MQTT-3.1.2-9]", "If the Will Flag is set to " +
                        "1, the Will QoS and Will Retain fields in the Connect Flags will be used by the Server, " +
                        "and the Will Properties, Will Topic and Will Message fields MUST be present in the Payload."
            )
        }
        if (variableHeader.hasUserName && payload.userName == null) {
            return MqttWarning(
                "[MQTT-3.1.2-17]", "If the User Name Flag is set" +
                        " to 1, a User Name MUST be present in the Payload"
            )
        }
        if (!variableHeader.hasUserName && payload.userName != null) {
            return MqttWarning(
                "[MQTT-3.1.2-16]", "If the User Name Flag is set " +
                        "to 0, a User Name MUST NOT be present in the Payload"
            )
        }
        if (variableHeader.hasPassword && payload.password == null) {
            return MqttWarning(
                "[MQTT-3.1.2-19]", "If the Password Flag is set" +
                        " to 1, a Password MUST be present in the Payload"
            )
        }
        if (!variableHeader.hasPassword && payload.password != null) {
            return MqttWarning(
                "[MQTT-3.1.2-18]", "If the Password Flag is set " +
                        "to 0, a Password MUST NOT be present in the Payload"
            )
        }
        return null
    }
}
