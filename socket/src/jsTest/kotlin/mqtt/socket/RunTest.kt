package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun <T> block(body: suspend CoroutineScope.() -> T): dynamic = GlobalScope.promise { body() }.catch {
    if (it !is UnsupportedOperationException) {
        throw it
    }
}
