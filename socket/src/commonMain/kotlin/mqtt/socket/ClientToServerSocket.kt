package mqtt.socket

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalUnsignedTypes
interface ClientToServerSocket : ClientSocket {
    suspend fun open(
        timeout: Duration,
        port: UShort,
        hostname: String? = null,
        socketOptions: SocketOptions? = null
    ): SocketOptions
}