package mqtt.socket

import kotlinx.coroutines.*

actual fun <T> block(body: suspend CoroutineScope.() -> T) {
    runTestInternal(block = {
        body()
    })
}


fun runTestInternal(
    block: suspend CoroutineScope.() -> Unit
): dynamic {
    return GlobalScope.promise(block = block, context = CoroutineExceptionHandler { context, e ->
        if (e is CancellationException) return@CoroutineExceptionHandler // are ignored
    }).catch { e ->
        if (e !is UnsupportedOperationException) {
            throw e
        }
    }
}