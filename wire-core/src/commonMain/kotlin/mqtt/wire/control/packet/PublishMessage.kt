package mqtt.wire.control.packet

import mqtt.wire.control.packet.format.ReasonCode
import mqtt.wire.data.QualityOfService

interface IPublishMessage : ControlPacket {
    val qualityOfService: QualityOfService
    val topic: CharSequence
    val packetIdentifier: UShort?
    fun expectedResponse(
        reasonCode: ReasonCode = ReasonCode.SUCCESS,
        reasonString: CharSequence? = null,
        userProperty: List<Pair<CharSequence, CharSequence>> = emptyList()
    ): ControlPacket?

    companion object {
        const val controlPacketValue: Byte = 3
    }
}
