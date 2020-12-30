package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.promise
import kotlin.js.Promise
import kotlinx.browser.window

actual fun <T> block(body: suspend CoroutineScope.() -> T): dynamic = runTestInternal(block = body)


fun <T> runTestInternal(
    block: suspend CoroutineScope.() -> T
): Promise<T?> {
    val promise = GlobalScope.promise {
        if (!isNodeJs) {
            return@promise null
        }
        try {
            return@promise block()
        } catch (e: UnsupportedOperationException) {
            println("unsupported operation")
        } catch (e: Exception) {
            cancel("failed promise", e)
        }
        return@promise null
    }
    promise.catch {
        if (it !is UnsupportedOperationException) {
            throw it
        }
    }
    return promise
}

val isNodeJs by lazy {
    try {
        window
        false
    } catch (t: Throwable) {
        true
    }
}