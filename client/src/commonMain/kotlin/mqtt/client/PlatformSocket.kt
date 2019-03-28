package mqtt.client

import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel

interface PlatformSocket {
    val output: ByteWriteChannel
    val input: ByteReadChannel
    fun dispose()
    suspend fun awaitClosed()
    val isClosed: Boolean
}
