package mqtt.transport.nio.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.core.readByteBuffer
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IConnectionRequest
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Performs [AsynchronousSocketChannel.connect] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */

suspend fun AsynchronousSocketChannel.aConnect(
    socketAddress: SocketAddress
) = suspendCancellableCoroutine<Unit> { cont ->
    connect(socketAddress, cont, AsyncVoidIOHandler)
    closeOnCancel(cont)
}

/**
 * Performs [AsynchronousSocketChannel.read] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */

@ExperimentalTime
suspend fun AsynchronousSocketChannel.aRead(
    buf: ByteBuffer,
    duration: Duration
) = suspendCancellableCoroutine<Int> { cont ->
    read(
        buf, duration.toLongMilliseconds(), TimeUnit.MILLISECONDS, cont,
        asyncIOHandler()
    )
    closeOnCancel(cont)
}

@ExperimentalTime
suspend fun AsynchronousSocketChannel.aReadPacket(
    buf: ByteBuffer,
    scope: CoroutineScope,
    protocolVersion: Int,
    timeout: Duration
): ControlPacket {
    var bytesRead = aRead(buf, timeout)
    while (scope.isActive && bytesRead < 2) {
        bytesRead = aRead(buf, timeout)
    }
    return suspendCancellableCoroutine { contination ->
        if (!scope.isActive || !isOpen) {
            contination.cancel()
            return@suspendCancellableCoroutine
        }
        try {
            buf.flip()
            val position = buf.position()
            // skip first byte, this will get validated later
            buf.get()
            val remainingLength = buf.decodeVariableByteInteger()
            buf.position(position)
            if (remainingLength.toLong() < buf.remaining()) { // we already read the entire message in the buffer
                val pkt = buf.read(protocolVersion)
                contination.resume(pkt)
            } else {
                throw UnsupportedOperationException("TODO: WIP to read buffers larger than whats larger than max buffer")
            }
        } catch (ex: Throwable) {
            println("read failed $buf $ex")
            ex.printStackTrace()
            contination.cancel()
        } finally {
            closeOnCancel(contination)
        }
    }
}


@ExperimentalTime
suspend fun AsynchronousSocketChannel.readConnectionRequest(
    packetBuffer: ByteBuffer,
    timeout: Duration
): IConnectionRequest? {
    aRead(packetBuffer, timeout)
    return packetBuffer.readConnectionRequest()
}

@UseExperimental(ExperimentalTime::class)
suspend fun AsynchronousSocketChannel.writePacket(packet: ControlPacket, timeout: Duration): Int {
    val bytes = aWrite(
        packet.serialize().readByteBuffer(direct = true),
        timeout
    )
    return bytes
}

/**
 * Performs [AsynchronousSocketChannel.write] without blocking a thread and resumes when asynchronous operation completes.
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */

@ExperimentalTime
suspend fun AsynchronousSocketChannel.aWrite(
    buf: ByteBuffer,
    duration: Duration
): Int {
    return suspendCancellableCoroutine<Int> { cont ->
        write(
            buf, duration.toLongMilliseconds(), TimeUnit.MILLISECONDS, cont,
            asyncIOHandler()
        )
        closeOnCancel(cont)
    }
}

/**
 * Performs [AsynchronousSocketChannel.close] without blocking a thread and resumes when asynchronous operation completes.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * *closes the underlying channel* and immediately resumes with [CancellationException].
 */

suspend fun AsynchronousSocketChannel.aClose() {
    suspendCancellableCoroutine<Void?> { cont ->
        blockingClose()
        cont.resume(null)
    }
}


fun AsynchronousSocketChannel.assignedPort(): UShort? {
    return try {
        (remoteAddress as? InetSocketAddress)?.port?.toUShort()
    } catch (e: Exception) {
        null
    }
}


suspend fun String?.asInetAddress() = suspendCoroutine<InetAddress> {
    try {
        it.resume(InetAddress.getByName(this))
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }
}

internal fun AsynchronousSocketChannel.blockingClose() {
    try {
        shutdownOutput()
    } catch (ex: Throwable) {
    }
    try {
        shutdownInput()
    } catch (ex: Throwable) {
    }
    try {
        close()
    } catch (ex: Throwable) {
        // Specification says that it is Ok to call it any time, but reality is different,
        // so we have just to ignore exception
    }
}
