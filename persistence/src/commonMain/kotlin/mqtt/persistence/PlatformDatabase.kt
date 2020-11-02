package mqtt.persistence

interface PlatformDatabase {
    suspend fun open(tables: Map<String, Row>): Map<String, PlatformTable>
    suspend fun dropTable(table: PlatformTable)
}

expect fun getPlatformDatabase(name: String): PlatformDatabase