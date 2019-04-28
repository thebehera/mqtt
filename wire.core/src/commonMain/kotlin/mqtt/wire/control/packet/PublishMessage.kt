package mqtt.wire.control.packet

import mqtt.wire.data.QualityOfService

interface IPublishMessage : ControlPacket {
    val qualityOfService: QualityOfService
    fun expectedResponse(): ControlPacket?
}
