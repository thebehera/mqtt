package mqtt.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.io.core.Closeable
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

fun CoroutineScope.blockWithTimeout(closeable: Closeable, timeoutMs: Long, body: suspend () -> Unit) {
    blockWithTimeout(timeoutMs) {
        body()
        println("run close")
        closeable.close()
    }
}