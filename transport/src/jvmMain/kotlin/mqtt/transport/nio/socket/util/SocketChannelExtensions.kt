package mqtt.transport.nio.socket.util

import kotlinx.coroutines.*
import mqtt.time.currentTimestampMs
import mqtt.transport.nio2.util.closeOnCancel
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
suspend fun SocketChannel.suspendUntilReady(
    scope: CoroutineScope,
    selector: Selector,
    ops: Int,
    timeout: Duration? = null
) {
    val random = random()
    suspendCancellableCoroutine<Double> {
        register(selector, ops, WrappedContinuation(it, random))
        scope.launch {
            selector.select(scope, random, timeout)
        }
    }
}

@ExperimentalTime
suspend fun Selector.select(scope: CoroutineScope, attachment: Any, timeout: Duration?) {
    while (aSelect(timeout) == 0) {
        if (!(scope.isActive && isOpen)) {
            return
        }
        continue
    }
    val selectedKeys = selectedKeys().iterator()
    while (selectedKeys.hasNext()) {
        val key = selectedKeys.next() as SelectionKey
        val keyAttachment = key.attachment() as? WrappedContinuation<*> ?: continue
        if (keyAttachment.attachment == attachment) {
            if (key.isValid) {
                keyAttachment.resume()
            } else {
                keyAttachment.cancel()
            }
            selectedKeys.remove()
            return
        }
    }
}


suspend fun SocketChannel.aConnect(remote: SocketAddress) = if (isBlocking) {
    withContext(Dispatchers.IO) {
        println("blocking suspend connect")
        suspendConnect(remote)
    }
} else {
    println("async suspend connect")
    suspendConnect(remote)
}

private suspend fun SocketChannel.suspendConnect(remote: SocketAddress) = suspendCancellableCoroutine<Boolean> {
    try {
        println("connect $remote")
        it.resume(connect(remote))
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }
}

@ExperimentalTime
suspend fun SocketChannel.connect(
    scope: CoroutineScope,
    remote: SocketAddress,
    selector: Selector? = null,
    timeout: Duration? = null
): Boolean {
    println("${currentTimestampMs()} remote $remote")
    val connected = aConnect(remote)
    println("${currentTimestampMs()} remote $connected")
    if (selector != null && !isBlocking) {
        suspendUntilReady(scope, selector, SelectionKey.OP_CONNECT, timeout)
    }
    println("suspend done")
    if (connected || aFinishConnecting()) {
        return true
    }
    throw TimeoutException("${currentTimestampMs()} Failed to connect to $remote within $timeout maybe invalid selector")
}

suspend fun SocketChannel.aFinishConnecting() = suspendCancellableCoroutine<Boolean> {
    try {
        println("a finish connecting")
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
    scope: CoroutineScope,
    timeout: Duration?
) {
    if (isBlocking) {
        return
    }
    val selectorNonNull =
        selector ?: throw IllegalArgumentException("Selector must be provided if it is a non-blocking channel")
    suspendUntilReady(scope, selectorNonNull, op, timeout)
}

@ExperimentalTime
suspend fun SocketChannel.read(
    scope: CoroutineScope,
    buffer: ByteBuffer,
    selector: Selector?,
    timeout: Duration?
): Int {
    return if (isBlocking) {
        withContext(Dispatchers.IO) {
            return@withContext suspendRead(buffer)
        }
    } else {
        suspendNonBlockingSelector(selector, SelectionKey.OP_READ, scope, timeout)
        suspendRead(buffer)
    }
}

@ExperimentalTime
suspend fun SocketChannel.write(
    scope: CoroutineScope,
    buffer: ByteBuffer,
    selector: Selector?,
    timeout: Duration?
): Int {
    return if (isBlocking) {
        suspendWrite(buffer)
    } else {
        suspendNonBlockingSelector(selector, SelectionKey.OP_WRITE, scope, timeout)
        suspendWrite(buffer)
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