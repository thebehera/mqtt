package mqtt.socket.nio.util

import kotlinx.coroutines.*
import java.lang.Math.random
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock
import kotlin.time.milliseconds


suspend fun openSocketChannel(remote: SocketAddress? = null) = suspendCoroutine<SocketChannel> {
    try {

        it.resume(
            if (remote == null) {
                SocketChannel.open()
            } else {
                SocketChannel.open(remote)
            }
        )
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }
}

data class WrappedContinuation<T>(val continuation: CancellableContinuation<T>, val attachment: T) {
    fun resume() = continuation.resume(attachment)
    fun cancel() = continuation.cancel()
}

@ExperimentalTime
suspend fun SocketChannel.suspendUntilReady(selector: Selector, ops: Int, timeout: Duration) {
    val random = random()
    suspendCancellableCoroutine<Double> {
        val key = register(selector, ops, WrappedContinuation(it, random))
        runBlocking {
            selector.select(key, random, timeout)
        }
    }
}

@ExperimentalTime
suspend fun Selector.select(selectionKey: SelectionKey, attachment: Any, timeout: Duration) {
    val startTime = MonoClock.markNow()
    val selectedCount = aSelect(timeout)
    if (selectedCount == 0) {
        throw CancellationException("Selector timed out after waiting $timeout for ${selectionKey.isConnectable}")
    }
    while (isOpen && timeout - startTime.elapsedNow() > 0.milliseconds) {
        if (selectedKeys().remove(selectionKey)) {
            val cont = selectionKey.attachment() as WrappedContinuation<*>
            if (cont.attachment != attachment) {
                throw IllegalStateException("Continuation attachment was mutated!")
            }
            if (selectionKey.isValid) {
                cont.resume()
            } else {
                cont.cancel()
            }
            return
        }
    }

    throw CancellationException("Failed to find selector in time")
}


suspend fun SocketChannel.aConnect(remote: SocketAddress) = if (isBlocking) {
    withContext(Dispatchers.IO) {
        suspendConnect(remote)
    }
} else {
    suspendConnect(remote)
}

private suspend fun SocketChannel.suspendConnect(remote: SocketAddress) = suspendCancellableCoroutine<Boolean> {
    try {
        it.resume(connect(remote))
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }
}

@ExperimentalTime
suspend fun SocketChannel.connect(
    remote: SocketAddress,
    selector: Selector? = null,
    timeout: Duration
): Boolean {
    val connected = aConnect(remote)
    if (selector != null && !isBlocking) {
        suspendUntilReady(selector, SelectionKey.OP_CONNECT, timeout)
    }
    if (connected || aFinishConnecting()) {
        return true
    }
    throw TimeoutException("${MonoClock.markNow()} Failed to connect to $remote within $timeout maybe invalid selector")
}

suspend fun SocketChannel.aFinishConnecting() = suspendCancellableCoroutine<Boolean> {
    try {
        it.resume(finishConnect())
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }
}

suspend fun SelectableChannel.aConfigureBlocking(block: Boolean) = suspendCancellableCoroutine<SelectableChannel> {
    try {
        it.resume(configureBlocking(block))
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }
}

@ExperimentalTime
private suspend fun SocketChannel.suspendNonBlockingSelector(
    selector: Selector?,
    op: Int,
    timeout: Duration
) {
    if (isBlocking) {
        return
    }
    val selectorNonNull =
        selector ?: throw IllegalArgumentException("Selector must be provided if it is a non-blocking channel")
    suspendUntilReady(selectorNonNull, op, timeout)
}

@ExperimentalTime
suspend fun SocketChannel.read(
    buffer: ByteBuffer,
    selector: Selector?,
    timeout: Duration
): Int {
    return if (isBlocking) {
        withContext(Dispatchers.IO) {
            return@withContext suspendRead(buffer)
        }
    } else {
        suspendNonBlockingSelector(selector, SelectionKey.OP_READ, timeout)
        suspendRead(buffer)
    }
}

@ExperimentalTime
suspend fun SocketChannel.write(
    buffer: ByteBuffer,
    selector: Selector?,
    timeout: Duration
): Int {
    return if (isBlocking) {
        suspendWrite(buffer)
    } else {
        suspendNonBlockingSelector(selector, SelectionKey.OP_WRITE, timeout)
        suspendWrite(buffer)
    }
}

@ExperimentalTime
fun NetworkChannel.closeOnCancel(cont: CancellableContinuation<*>) {
    cont.invokeOnCancellation {
        blockingClose()
    }
}

@ExperimentalTime
private suspend fun SocketChannel.suspendRead(buffer: ByteBuffer) = suspendCancellableCoroutine<Int> {
    try {
        val read = read(buffer)
        it.resume(read)
    } catch (ex: Throwable) {
        if (ex is AsynchronousCloseException && it.isCancelled) return@suspendCancellableCoroutine
        closeOnCancel(it)
    }
}

@ExperimentalTime
private suspend fun SocketChannel.suspendWrite(buffer: ByteBuffer) = suspendCancellableCoroutine<Int> {
    try {
        val wrote = write(buffer)
        it.resume(wrote)
    } catch (ex: Throwable) {
        if (ex is AsynchronousCloseException && it.isCancelled) return@suspendCancellableCoroutine
        closeOnCancel(it)
    }
}