package mqtt.transport.nio2.util

import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun AsynchronousServerSocketChannel.aAccept() = suspendCoroutine<AsynchronousSocketChannel> { cont ->
    accept(cont, object : CompletionHandler<AsynchronousSocketChannel, Continuation<AsynchronousSocketChannel>> {
        override fun completed(result: AsynchronousSocketChannel, attachment: Continuation<AsynchronousSocketChannel>) {
            cont.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Continuation<AsynchronousSocketChannel>) {
            cont.resumeWithException(exc)
        }

    })
}

/**
 * Performs [AsynchronousServerSocketChannel.bind] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */

suspend fun AsynchronousServerSocketChannel.aBind(socketAddress: SocketAddress?) =
    suspendCancellableCoroutine<AsynchronousServerSocketChannel> { cont ->
        try {
            closeOnCancel(cont)
            cont.resume(bind(socketAddress))
        } catch (e: Throwable) {
            cont.cancel(e)
        }
    }
