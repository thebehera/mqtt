package mqtt.socket

import kotlinx.coroutines.CoroutineScope

actual fun <T> block(body: suspend CoroutineScope.() -> T) {
    /**ignored**/
}