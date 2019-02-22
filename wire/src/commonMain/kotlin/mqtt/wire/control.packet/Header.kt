package mqtt.wire.control.packet

interface FixedHeader {
    val controlPacketType: ControlPacketType
    val x: String
}

