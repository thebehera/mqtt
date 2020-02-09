package mqtt.transport.nio.socket.util

import kotlinx.coroutines.*
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

suspend fun SocketChannel.aRegister(selector: Selector, ops: Int, attachment: Any? = null) =
    suspendCoroutine<SelectionKey> {
        it.resume(register(selector, ops, attachment))
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
        println("connect $remote")
        it.resume(connect(remote))
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }
}

@ExperimentalTime
suspend fun Selector.selectForKey(scope: CoroutineScope, attachment: Any, timeout: Duration?): SelectionKey? {
    var key = selectedKeys().firstOrNull { it.attachment() == attachment }
    var timeLeftMs = timeout?.toLongMilliseconds() ?: Long.MAX_VALUE
    while (scope.isActive && key == null) {
        aSelect(timeout)
        key = selectedKeys().firstOrNull { it.attachment() == attachment }
    }
    if (key != null) {
        selectedKeys().remove(key)
    }
    return key
}

@ExperimentalTime
suspend fun SocketChannel.connect(
    scope: CoroutineScope,
    remote: SocketAddress,
    selector: Selector? = null,
    timeout: Duration? = null
): Boolean {
    suspendNonBlockingSelector(selector, SelectionKey.OP_CONNECT, scope, timeout)
    println("remote $remote")
    val connected = aConnect(remote)
    if (connected || aFinishConnecting()) {
        return true
    }
    throw TimeoutException("Failed to connect to $remote within $timeout maybe invalid selector")
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
    scope: CoroutineScope,
    timeout: Duration?
) {
    if (isBlocking) {
        return
    }
    if (selector == null) {
        throw IllegalArgumentException("Selector must be provided if it is a non-blocking channel")
    }
    val attachment = random()
    aRegister(selector, op, attachment)
    selector.selectForKey(scope, attachment, timeout)
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