@file:JvmName("RunTest")

package mqtt.persistence

import kotlinx.coroutines.runBlocking

actual fun <T> runTest(block: suspend () -> T) {
    runBlocking { block() }
}