package mqtt.transport.nio.util

import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun AsynchronousChannelGroup?.openAsyncServerSocketChannel(): AsynchronousServerSocketChannel =
    suspendCancellableCoroutine { continuation ->
        try {
            continuation.resume(AsynchronousServerSocketChannel.open(this))
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