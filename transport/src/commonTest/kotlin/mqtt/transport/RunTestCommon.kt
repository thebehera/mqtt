package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext

expect fun <T> block(body: suspend CoroutineScope.() -> T)
expect fun <T> block(context: CoroutineContext, body: suspend CoroutineScope.() -> T)


fun <T> blockWithTimeout(timeoutMs: Long = 1000L, body: suspend CoroutineScope.() -> T) {
    block {
        withTimeout(timeoutMs) {
            body()
        }
    }
}

fun <T> CoroutineScope.blockWithTimeout(timeoutMs: Long = 1000L, body: suspend CoroutineScope.() -> T) {
    block(this.coroutineContext) {
        withTimeout(timeoutMs) {
            body()
        }
    }
}