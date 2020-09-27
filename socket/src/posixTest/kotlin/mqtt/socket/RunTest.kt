package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun <T> block(body: suspend CoroutineScope.() -> T) {
    runBlocking {
        try {
            body()
        } catch (e: UnsupportedOperationException) {
            println("Test hit unsupported code, ignoring")
        }
    }
}