package mqtt.persistence

import com.squareup.sqldelight.db.SqlDriver
import mqtt.persistence.db.Database

actual fun getPlatformDatabase(name: String, contextProvider: ContextProvider): PlatformDatabase =
    throw UnsupportedOperationException()


actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(Database.Schema, "test.db")
    }
}