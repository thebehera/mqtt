package mqtt.transport.nio2.util

import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun openAsyncServerSocketChannel(group: AsynchronousChannelGroup? = null): AsynchronousServerSocketChannel =
    suspendCancellableCoroutine { continuation ->
        try {
            continuation.resume(AsynchronousServerSocketChannel.open(group))
            println("opened server socket channel")
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }


suspend fun AsynchronousChannelGroup?.open(): AsynchronousSocketChannel =
    suspendCancellableCoroutine {
        try {
            it.resume(AsynchronousSocketChannel.open(this))
        } catch (e: Throwable) {
            it.resumeWithException(e)
        }
    }