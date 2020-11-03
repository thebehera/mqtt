@file:JvmName("RunTest")

package mqtt.persistence

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking

actual fun <T> runTest(block: suspend (ContextProvider) -> T) {

}

fun <T> runAndroidTest(block: suspend (ContextProvider) -> T) {
    runBlocking {
        try {
            block(AndroidContextProvider(InstrumentationRegistry.getInstrumentation().context))
        } catch (e: UnsupportedOperationException) {
            println("ignore")
        }
    }
}