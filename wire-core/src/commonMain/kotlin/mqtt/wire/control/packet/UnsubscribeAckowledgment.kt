package mqtt.wire.control.packet

interface IUnsubscribeAckowledgment : ControlPacket {
    val packetIdentifier: Int
}