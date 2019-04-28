package mqtt.wire.control.packet

interface ISubscribeRequest : ControlPacket {
    fun expectedResponse(): ISubscribeAcknowledgement
}
