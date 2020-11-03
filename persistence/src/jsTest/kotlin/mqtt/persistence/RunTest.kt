package mqtt.persistence

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun <T> runTest(block: suspend (ContextProvider) -> T): dynamic = code(block)

fun <T> code(block: suspend (ContextProvider) -> T) = GlobalScope.promise {
    try {
        block(ContextProvider())
        console.log("done blocking\n")
    } catch (t: Throwable) {
        console.error("Failed", t)
    } finally {
        console.log("finally done")
    }
}
