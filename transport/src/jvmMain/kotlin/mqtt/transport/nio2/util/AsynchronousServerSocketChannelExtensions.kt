package mqtt.transport.nio2.util

import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import kotlin.coroutines.resume

/**
 * Performs [AsynchronousServerSocketChannel.accept] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */

suspend fun AsynchronousServerSocketChannel.aAccept() = suspendCancellableCoroutine<AsynchronousSocketChannel> { cont ->
    accept(cont, asyncIOHandler())
    closeOnCancel(cont)
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
