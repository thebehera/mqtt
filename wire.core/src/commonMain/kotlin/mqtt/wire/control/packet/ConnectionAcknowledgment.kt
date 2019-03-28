package mqtt.wire.control.packet

interface IConnectionAcknowledgment : ControlPacket {
    val isSuccessful: Boolean
    val connectionReason: String
}
