package mqtt.wire.control.packet

import mqtt.wire.data.QualityOfService

interface IPublishMessage : ControlPacket {
    val qualityOfService: QualityOfService
    val topic: CharSequence
    fun expectedResponse(): ControlPacket?

    companion object {
        const val controlPacketValue: Byte = 3
    }
}
