package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

interface BufferPool<Buffer> {
    fun borrow(): Buffer
}

interface SuspendCloseable {
    suspend fun close()
}

@ExperimentalTime
@ExperimentalUnsignedTypes
interface ClientSocket<Buffer> : SuspendCloseable {
    val scope: CoroutineScope
    val pool: BufferPool<Buffer>
    var readTimeout: Duration // can be changed during the course of the connection
    var writeTimeout: Duration
    val incoming: Flow<Buffer>
    fun isOpen(): Boolean
    fun localPort(): UShort?
    fun remotePort(): UShort?
    suspend fun send(buffer: Buffer)
    var tag: Any?
}

@ExperimentalTime
@ExperimentalUnsignedTypes
interface ClientToServerSocket<Buffer> : ClientSocket<Buffer> {
    suspend fun open(hostname: String? = null, port: UShort)
}

@ExperimentalTime
@ExperimentalUnsignedTypes
interface ServerToClientSocket<Buffer> : SuspendCloseable {
    val scope: CoroutineScope
    fun port(): UShort?
    suspend fun bind(port: UShort? = null, host: String? = null)
    suspend fun listen(): Flow<ClientSocket<Buffer>>
}


@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun asyncClientSocket(
    coroutineScope: CoroutineScope,
    readTimeout: Duration,
    writeTimeout: Duration
): ClientToServerSocket<*>


@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ExperimentalTime
expect fun asyncServerSocket(
    coroutineScope: CoroutineScope,
    version: Int,
    readTimeout: Duration,
    writeTimeout: Duration
): ServerToClientSocket<*>

@ExperimentalUnsignedTypes
@ExperimentalTime
@ExperimentalCoroutinesApi
expect fun clientSocket(
    coroutineScope: CoroutineScope,
    blocking: Boolean,
    readTimeout: Duration,
    writeTimeout: Duration
): ClientToServerSocket<*>