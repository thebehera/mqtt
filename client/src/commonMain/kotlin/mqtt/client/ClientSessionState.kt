package mqtt.client

import mqtt.wire.control.packet.ControlPacket

class ClientSessionState {
    val qos1And2MessagesSentButNotAcked = LinkedHashSet<ControlPacket>()
    val qos2MessagesRecevedBytNotCompletelyAcked = LinkedHashSet<ControlPacket>()
}

