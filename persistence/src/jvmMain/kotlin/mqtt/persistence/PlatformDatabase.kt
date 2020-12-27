package mqtt.persistence

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver

actual fun getPlatformDatabase(name: String, contextProvider: ContextProvider): PlatformDatabase = JvmSqlDatabase(name)

actual fun createDriver(name: String, contextProvider: ContextProvider): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
