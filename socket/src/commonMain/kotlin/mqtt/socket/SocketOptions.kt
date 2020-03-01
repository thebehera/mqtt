package mqtt.socket

data class SocketOptions(
    val tcpNoDelay: Boolean? = null,
    val reuseAddress: Boolean? = null,
    val keepAlive: Boolean? = null,
    val receiveBuffer: UInt? = null,
    val sendBuffer: UInt? = null
)