package mqtt.client.service

interface OnClientMessageReceived {
    fun onMessage(obj: Any)
}