package mqtt.persistence

import com.squareup.sqldelight.db.SqlDriver

interface PlatformDatabase {
    suspend fun open(tables: Map<String, Row>): Map<String, PlatformTable>
    suspend fun dropTable(table: PlatformTable)
}

open class ContextProvider

expect fun getPlatformDatabase(name: String, contextProvider: ContextProvider = ContextProvider()): PlatformDatabase

expect fun createDriver(name: String, contextProvider: ContextProvider = ContextProvider()): SqlDriver