package mqtt.socket

import kotlinx.coroutines.CoroutineScope

expect fun <T> block(body: suspend CoroutineScope.() -> T)

fun <T> blockIgnoreUnsupported(body: suspend CoroutineScope.() -> T) {
    try {
        block(body)
    } catch (e: UnsupportedOperationException) {
        // ignore
    }
}
