package mqtt.socket

interface SuspendCloseable {
    suspend fun close()
}