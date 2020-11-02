package mqtt.persistence

actual fun getPlatformDatabase(name: String): PlatformDatabase = JvmSqlDatabase(name)