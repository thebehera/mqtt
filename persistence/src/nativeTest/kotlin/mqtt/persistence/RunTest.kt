package mqtt.persistence

import kotlinx.coroutines.runBlocking

actual fun <T> runTest(block: suspend () -> T): Any {
    runBlocking { block() }
    return Unit
}