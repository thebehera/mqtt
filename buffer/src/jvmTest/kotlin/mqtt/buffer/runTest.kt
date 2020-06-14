@file:JvmName("RunTest")

package mqtt.buffer

import kotlinx.coroutines.runBlocking

actual fun <T> runTest(block: suspend () -> T) {
    runBlocking { block() }
}