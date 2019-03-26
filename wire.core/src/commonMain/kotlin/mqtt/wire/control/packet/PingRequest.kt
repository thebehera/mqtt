package mqtt.wire.control.packet

interface IPingRequest : ControlPacket {
    val lazyBytes: ByteArray
}
