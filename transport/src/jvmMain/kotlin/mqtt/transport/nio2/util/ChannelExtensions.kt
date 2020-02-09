package mqtt.transport.nio2.util

import kotlinx.coroutines.CancellableContinuation
import mqtt.transport.nio.socket.util.blockingClose
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.Channel
import java.nio.channels.NetworkChannel
import kotlin.time.ExperimentalTime

fun Channel.blockingClose() {
    try {
        close()
    } catch (ex: Throwable) {
        // Specification says that it is Ok to call it any time, but reality is different,
        // so we have just to ignore exception
    }
}


fun AsynchronousFileChannel.closeOnCancel(cont: CancellableContinuation<*>) {
    cont.invokeOnCancellation {
        blockingClose()
    }
}

@ExperimentalTime
fun NetworkChannel.closeOnCancel(cont: CancellableContinuation<*>) {
    cont.invokeOnCancellation {
        blockingClose()
    }
}