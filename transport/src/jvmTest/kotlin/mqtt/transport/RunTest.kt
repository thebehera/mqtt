package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

actual fun <T> block(body: suspend CoroutineScope.() -> T) {
    runBlocking(block = body)
}

actual fun <T> block(context: CoroutineContext, body: suspend CoroutineScope.() -> T) {
    runBlocking(context, body)
}