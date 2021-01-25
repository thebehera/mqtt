@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket.nio2.util

import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.InetSocketAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


suspend fun asyncSocket(group: AsynchronousChannelGroup? = null) = suspendCoroutine<AsynchronousSocketChannel> {
    try {
        it.resume(AsynchronousSocketChannel.open(group))
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }
}


/**
 * Performs [AsynchronousSocketChannel.connect] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */
@ExperimentalTime
suspend fun AsynchronousSocketChannel.aConnect(
    socketAddress: SocketAddress
) = withContext(Dispatchers.IO) {
    suspendCoroutine<Unit> { cont ->
        connect(socketAddress, cont, AsyncVoidIOHandler())
    }
}

/**
 * Performs [AsynchronousSocketChannel.read] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */

@ExperimentalTime
suspend fun AsynchronousSocketChannel.aRead(
    buf: ByteBuffer,
    duration: Duration
)= withContext(Dispatchers.IO)  {
    val result = suspendCancellableCoroutine<Int> { cont ->
        read(
            buf, duration.toLongMilliseconds(), TimeUnit.MILLISECONDS, cont,
            asyncIOIntHandler()
        )
        closeOnCancel(cont)
    }
    buf.flip()
    result
}

/**
 * Performs [AsynchronousSocketChannel.write] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */

@ExperimentalTime
suspend fun AsynchronousSocketChannel.aWrite(
    buf: ByteBuffer,
    duration: Duration
) = withContext(Dispatchers.IO) {
    suspendCancellableCoroutine<Int> { cont ->
        buf.flip()
        write(
            buf, duration.toLongMilliseconds(), TimeUnit.MILLISECONDS, cont,
            asyncIOHandler()
        )
        closeOnCancel(cont)
    }
}

/**
 * Performs [AsynchronousSocketChannel.close] without blocking a thread and resumes when asynchronous operation completes.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */

@ExperimentalTime
suspend fun AsynchronousSocketChannel.aClose()= withContext(Dispatchers.IO) {
    suspendCoroutine<Unit> { cont ->
        blockingClose()
        cont.resume(Unit)
    }
}


fun AsynchronousSocketChannel.assignedPort(remote: Boolean = true): UShort? {
    return try {
        if (remote) {
            (remoteAddress as? InetSocketAddress)?.port?.toUShort()
        } else {
            (localAddress as? InetSocketAddress)?.port?.toUShort()
        }
    } catch (e: Exception) {
        null
    }
}
