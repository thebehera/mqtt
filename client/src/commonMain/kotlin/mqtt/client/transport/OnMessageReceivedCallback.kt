package mqtt.client.transport

import mqtt.wire.control.packet.ControlPacket

interface OnMessageReceivedCallback {
    fun onMessage(controlPacket: ControlPacket)
}