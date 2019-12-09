package mqtt.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout

expect fun <T> block(body: suspend CoroutineScope.() -> T)


fun <T> blockWithTimeout(timeoutMs: Long = 2000L, body: suspend CoroutineScope.() -> T) {
    block {
        withTimeout(timeoutMs) {
            body()
        }
    }
}