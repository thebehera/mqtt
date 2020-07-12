package mqtt.socket

import kotlinx.coroutines.*
import kotlin.js.Promise
import kotlin.test.Test

class RunTest {
    @Test
    fun testAsyncWorksCorrectly() = runTestInternal {
        println("hello")
        println("pre loop")
        delay(3400)
        println("we done son")

        println("start")
    }
}

actual fun <T> block(body: suspend CoroutineScope.() -> T): dynamic = runTestInternal(block = body)


fun <T> runTestInternal(
    block: suspend CoroutineScope.() -> T
): Promise<T?> {
    val promise = GlobalScope.promise {
        try {
            return@promise block()
        } catch (e: UnsupportedOperationException) {

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
