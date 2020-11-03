package mqtt.persistence

expect fun <T> runTest(block: suspend (ContextProvider) -> T)
