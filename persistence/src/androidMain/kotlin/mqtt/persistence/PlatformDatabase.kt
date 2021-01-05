package mqtt.persistence

import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import mqtt.persistence.db.Database

actual suspend fun getPlatformDatabase(name: String, contextProvider: ContextProvider): PlatformDatabase =
    AndroidSqlDatabase(name, contextProvider as AndroidContextProvider)

actual suspend fun createDriver(name: String, contextProvider: ContextProvider): SqlDriver =
    AndroidSqliteDriver(Database.Schema, (contextProvider as AndroidContextProvider).context, name)