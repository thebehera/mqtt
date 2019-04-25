package mqtt.client

import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel

interface Transport {
    val output: ByteWriteChannel
    val input: ByteReadChannel
    fun dispose()
    suspend fun awaitClosed()
    val isClosed: Boolean
    val isWebSocket: Boolean
}
