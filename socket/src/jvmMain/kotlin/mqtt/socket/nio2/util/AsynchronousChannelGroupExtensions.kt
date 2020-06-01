package mqtt.socket.nio2.util

import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun openAsyncServerSocketChannel(group: AsynchronousChannelGroup? = null): AsynchronousServerSocketChannel =
    suspendCancellableCoroutine { continuation ->
        try {
            continuation.resume(AsynchronousServerSocketChannel.open(group))
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
