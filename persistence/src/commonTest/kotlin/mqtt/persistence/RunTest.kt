package mqtt.persistence

expect fun <T> runTest(block: suspend () -> T): Any

fun <T> runTestBlocking(block: suspend () -> T) {
    runTest { block() }
}