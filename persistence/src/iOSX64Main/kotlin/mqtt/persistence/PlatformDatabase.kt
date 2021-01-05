package mqtt.persistence

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import mqtt.persistence.db.Database

actual suspend fun getPlatformDatabase(name: String, contextProvider: ContextProvider): PlatformDatabase =
    throw UnsupportedOperationException()

actual suspend fun createDriver(name: String, contextProvider: ContextProvider): SqlDriver =
    NativeSqliteDriver(Database.Schema, name)
