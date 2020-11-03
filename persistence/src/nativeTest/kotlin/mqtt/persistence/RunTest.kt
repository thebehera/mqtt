package mqtt.persistence

import kotlinx.coroutines.runBlocking

actual fun <T> runTest(block: suspend (ContextProvider) -> T) {
    runBlocking {
        try {
            block(ContextProvider())
        } catch (e: UnsupportedOperationException) {
            println("ignore")
        }
    }
}