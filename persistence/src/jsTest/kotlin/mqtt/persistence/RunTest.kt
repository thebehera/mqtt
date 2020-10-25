package mqtt.persistence

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun <T> runTest(block: suspend () -> T): dynamic = code(block)

fun <T> code(block: suspend () -> T)
    = GlobalScope.promise {
        try {
            block()
            console.log("done blocking\n")
        } catch (t: Throwable) {
            console.error("Failed", t)
        } finally {
            console.log("finally done")
        }
    }
