package mqtt.persistence

actual fun getPlatformDatabase(name: String, contextProvider: ContextProvider): PlatformDatabase =
    AndroidSqlDatabase(name, contextProvider as AndroidContextProvider)