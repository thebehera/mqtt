@file:JvmName("RunTest")

package mqtt.buffer

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.system.measureNanoTime
import kotlin.time.*

actual fun <T> runTest(block: suspend () -> T) {
    runBlocking { block() }
}

class Test123 {
    @ExperimentalTime
    @Test
    fun test() {
    }
}