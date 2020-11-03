package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import mqtt.buffer.BufferPool
import mqtt.buffer.PlatformBuffer
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class QueuedClientSocket(scope: CoroutineScope, val socket: ClientSocket, override val pool: BufferPool) :
    ClientSocket {
    private val writeQueue = Channel<Operation>(RENDEZVOUS)
    private val readQueue = Channel<Operation>(RENDEZVOUS)

    init {
        scope.launch {
            writeQueue.consumeAsFlow().collect { (buffer, timeout, callback) ->
                callback(socket.write(buffer, timeout))
            }
        }
        scope.launch {
            readQueue.consumeAsFlow().collect { (timeout, bufferRead, callback) ->
                callback(socket.read(timeout, bufferRead))
            }
        }
    }

    override suspend fun read(buffer: PlatformBuffer, timeout: Duration): Int {
        var bytesRead = 0
        readQueue.send(Operation(buffer, timeout) { bytesRead = it })
        return bytesRead
    }

    override suspend fun write(buffer: PlatformBuffer, timeout: Duration): Int {
        var bytesWritten = -1
        writeQueue.send(Operation(buffer, timeout) { bytesWritten = it })
        return bytesWritten
    }

    override fun isOpen() = socket.isOpen()
    override fun localPort() = socket.localPort()
    override fun remotePort() = socket.remotePort()
    override suspend fun close() {
        writeQueue.close()
        readQueue.close()
        socket.close()
    }
    private data class Operation(val buffer: PlatformBuffer, val timeout: Duration, val bytesWritten: (Int) -> Unit)
}
