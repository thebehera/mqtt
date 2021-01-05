package mqtt.persistence

import com.squareup.sqldelight.db.SqlDriver

actual suspend fun getPlatformDatabase(name: String, contextProvider: ContextProvider): PlatformDatabase {
    return JsIndexedDatabase(name)
}

actual suspend fun createDriver(name: String, contextProvider: ContextProvider): SqlDriver {
    throw UnsupportedOperationException()
}