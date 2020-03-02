package mqtt.buffer

interface SuspendCloseable {
    suspend fun close()
}