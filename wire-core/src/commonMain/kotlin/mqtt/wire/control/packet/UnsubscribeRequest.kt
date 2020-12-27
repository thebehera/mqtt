package mqtt.wire.control.packet

interface IUnsubscribeRequest : ControlPacket {
    val packetIdentifier: Int
}
