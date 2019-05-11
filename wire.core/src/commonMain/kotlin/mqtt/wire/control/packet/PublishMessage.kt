package mqtt.wire.control.packet

import kotlinx.io.core.ByteReadPacket
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name

interface IPublishMessage : ControlPacket {
    val qualityOfService: QualityOfService
    val topic: Name
    override fun payloadPacket(sendDefaults: Boolean): ByteReadPacket?
    fun expectedResponse(): ControlPacket?
}
