package mqtt.transport.nio2.util

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.SocketAddress
import java.net.SocketOption
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.ExperimentalTime

@ExperimentalTime
suspend fun AsynchronousServerSocketChannel.aAccept() = suspendCancellableCoroutine<AsynchronousSocketChannel> { cont ->
    accept(cont, AcceptCompletionHandler(cont))
}

data class AcceptCompletionHandler(val continuation: CancellableContinuation<AsynchronousSocketChannel>) :
    CompletionHandler<AsynchronousSocketChannel, CancellableContinuation<AsynchronousSocketChannel>> {
    override fun completed(
        result: AsynchronousSocketChannel,
        attachment: CancellableContinuation<AsynchronousSocketChannel>
    ) = continuation.resume(result)

    override fun failed(exc: Throwable, attachment: CancellableContinuation<AsynchronousSocketChannel>) {
        // just return if already cancelled and got an expected exception for that case
        if (exc is AsynchronousCloseException && continuation.isCancelled) return
        continuation.resumeWithException(exc)
    }

}


/**
 * Performs [AsynchronousServerSocketChannel.bind] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */

@ExperimentalTime
suspend fun AsynchronousServerSocketChannel.aBind(socketAddress: SocketAddress? = null, backlog: UInt = 0.toUInt()) =
    suspendCancellableCoroutine<AsynchronousServerSocketChannel> { cont ->
        try {
            closeOnCancel(cont)
            cont.resume(bind(socketAddress, backlog.toInt()))
        } catch (e: Throwable) {
            cont.cancel(e)
        }
    }


suspend fun <T> AsynchronousServerSocketChannel.asyncSetOption(option: SocketOption<T>, value: T) =
    suspendCoroutine<AsynchronousServerSocketChannel> {
        try {
            it.resume(setOption(option, value))
        } catch (e: Throwable) {
            it.resumeWithException(e)
        }
    }