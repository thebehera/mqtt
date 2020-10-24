package mqtt.persistence

actual fun getPlatformDatabase(name: String): PlatformDatabase = AndroidSqlDatabase(name)