package mqtt.persistence

actual fun getPlatformDatabase(name: String, contextProvider: ContextProvider): PlatformDatabase {
    return JsIndexedDatabase(name)
}