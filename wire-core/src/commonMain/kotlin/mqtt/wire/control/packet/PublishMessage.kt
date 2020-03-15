package mqtt.wire.control.packet

import kotlinx.io.core.ByteReadPacket
import mqtt.wire.data.QualityOfService

interface IPublishMessage : ControlPacket {
    val qualityOfService: QualityOfService
    val topic: CharSequence
    override fun payloadPacket(sendDefaults: Boolean): ByteReadPacket?
    fun expectedResponse(): ControlPacket?

    companion object {
        const val controlPacketValue: Byte = 3
    }
}
