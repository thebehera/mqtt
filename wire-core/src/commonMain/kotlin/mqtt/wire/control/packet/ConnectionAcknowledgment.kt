package mqtt.wire.control.packet

import mqtt.Parcelable

interface IConnectionAcknowledgment : ControlPacket, Parcelable {
    val isSuccessful: Boolean
    val connectionReason: String
    val sessionPresent: Boolean
}
