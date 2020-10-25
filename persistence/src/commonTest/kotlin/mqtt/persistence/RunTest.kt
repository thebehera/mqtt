package mqtt.persistence

expect fun <T> runTest(block: suspend () -> T)
