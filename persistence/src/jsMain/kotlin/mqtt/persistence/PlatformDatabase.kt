package mqtt.persistence

actual fun getPlatformDatabase(name: String): PlatformDatabase  {
    return JsIndexedDatabase(name)
}