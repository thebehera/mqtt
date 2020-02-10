package mqtt.transport.nio2.util

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import mqtt.time.currentTimestampMs
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
    val start = currentTimestampMs()
    println("$start      server wait for accept")
    accept(
        cont,
        object : CompletionHandler<AsynchronousSocketChannel, CancellableContinuation<AsynchronousSocketChannel>> {
            override fun completed(
                result: AsynchronousSocketChannel,
                attachment: CancellableContinuation<AsynchronousSocketChannel>
            ) {
                val deltaTime = currentTimestampMs() - start
                println("${currentTimestampMs()}      server $deltaTime ms to accept $result")
                cont.resume(result)
            }

            override fun failed(exc: Throwable, attachment: CancellableContinuation<AsynchronousSocketChannel>) {
//                println("accept failed with $exc after ${currentTimestampMs() - start}ms $this")
                // just return if already cancelled and got an expected exception for that case
                if (exc is AsynchronousCloseException && cont.isCancelled) return
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

@ExperimentalTime
suspend fun AsynchronousServerSocketChannel.aBind(socketAddress: SocketAddress?) =
    suspendCancellableCoroutine<AsynchronousServerSocketChannel> { cont ->
        try {
            closeOnCancel(cont)
            println("bind $socketAddress")
            cont.resume(bind(socketAddress))
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