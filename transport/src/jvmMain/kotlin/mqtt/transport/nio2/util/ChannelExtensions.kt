package mqtt.transport.nio2.util

import kotlinx.coroutines.CancellableContinuation
import java.nio.channels.Channel

private fun Channel.blockingClose() {
    try {
        close()
    } catch (ex: Throwable) {
        // Specification says that it is Ok to call it any time, but reality is different,
        // so we have just to ignore exception
    }
}


fun Channel.closeOnCancel(cont: CancellableContinuation<*>) {
    cont.invokeOnCancellation {
        blockingClose()
    }
}