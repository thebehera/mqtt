package mqtt.socket

import kotlin.time.ExperimentalTime

@ExperimentalTime
actual fun asyncClientSocket(): ClientToServerSocket {
    throw RuntimeException("")
}


@ExperimentalTime
actual fun clientSocket(blocking: Boolean): ClientToServerSocket {
    throw RuntimeException("")
}


@ExperimentalUnsignedTypes
@ExperimentalTime
actual fun asyncServerSocket(): ServerSocket {
    throw RuntimeException("")
}