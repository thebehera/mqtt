package mqtt.socket

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
@ExperimentalUnsignedTypes
interface ClientToServerSocket : ClientSocket {
    suspend fun open(
        port: UShort,
        timeout: Duration = 1.seconds,
        hostname: String = "localhost",
        socketOptions: SocketOptions? = null
    ): SocketOptions
}