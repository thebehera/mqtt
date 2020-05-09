@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket.nio.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mqtt.socket.SocketOptions
import java.net.SocketOption
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.NetworkChannel
import java.nio.channels.SocketChannel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.ExperimentalTime

suspend fun NetworkChannel.asyncSetOptions(options: SocketOptions?): SocketOptions {
    return withContext(Dispatchers.IO) {
        if (options != null) {
            if (options.tcpNoDelay != null && supportedOptions().contains(StandardSocketOptions.TCP_NODELAY)) {
                setOption(StandardSocketOptions.TCP_NODELAY, options.tcpNoDelay)
            }
            if (options.reuseAddress != null && supportedOptions().contains(StandardSocketOptions.SO_REUSEADDR)) {
                setOption(StandardSocketOptions.SO_REUSEADDR, options.reuseAddress)
            }
            if (options.keepAlive != null && supportedOptions().contains(StandardSocketOptions.SO_KEEPALIVE)) {
                setOption(StandardSocketOptions.SO_KEEPALIVE, options.keepAlive)
            }
            if (options.receiveBuffer != null && supportedOptions().contains(StandardSocketOptions.SO_RCVBUF)) {
                setOption(StandardSocketOptions.SO_RCVBUF, options.receiveBuffer.toInt())
            }
            if (options.sendBuffer != null && supportedOptions().contains(StandardSocketOptions.SO_SNDBUF)) {
                setOption(StandardSocketOptions.SO_SNDBUF, options.sendBuffer.toInt())
            }
        }
        SocketOptions(
            tryGettingOption(StandardSocketOptions.TCP_NODELAY),
            tryGettingOption(StandardSocketOptions.SO_REUSEADDR),
            tryGettingOption(StandardSocketOptions.SO_KEEPALIVE),
            tryGettingOption(StandardSocketOptions.SO_RCVBUF)?.toUInt(),
            tryGettingOption(StandardSocketOptions.SO_SNDBUF)?.toUInt()
        )
    }
}

fun <T> NetworkChannel.tryGettingOption(option: SocketOption<T>) = if (supportedOptions().contains(option)) {
    getOption(option)
} else {
    null
}

@ExperimentalTime
suspend fun NetworkChannel.aClose() {
    suspendCoroutine<Unit> { cont ->
        blockingClose()
        cont.resume(Unit)
    }
}


@ExperimentalTime
internal fun NetworkChannel.blockingClose() {
    try {
        if (this is SocketChannel) {
            shutdownInput()
        } else if (this is AsynchronousSocketChannel) {
            shutdownInput()
        }
    } catch (ex: Throwable) {
    }
    try {
        if (this is SocketChannel) {
            shutdownOutput()
        } else if (this is AsynchronousSocketChannel) {
            shutdownOutput()
        }
    } catch (ex: Throwable) {
    }
    try {
        close()
    } catch (ex: Throwable) {
        // Specification says that it is Ok to call it any time, but reality is different,
        // so we have just to ignore exception
    }
}