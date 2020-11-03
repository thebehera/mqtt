package mqtt.persistence

actual fun getPlatformDatabase(name: String, contextProvider: ContextProvider): PlatformDatabase =
    throw UnsupportedOperationException()