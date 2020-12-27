package mqtt.persistence

import com.squareup.sqldelight.db.SqlDriver

actual fun getPlatformDatabase(name: String, contextProvider: ContextProvider): PlatformDatabase {
    return JsIndexedDatabase(name)
}

actual fun createDriver(name: String, contextProvider: ContextProvider): SqlDriver =
    throw UnsupportedOperationException()