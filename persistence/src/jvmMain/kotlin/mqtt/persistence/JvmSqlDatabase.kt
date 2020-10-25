package mqtt.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

class JvmSqlDatabase(name: String) : PlatformDatabase {
    lateinit var connection: Connection
    override suspend fun open(tables: Map<String, Row>): Map<String, PlatformTable> {
        return emptyMap()
    }

    override suspend fun createTable(name: String, rowData: Row): PlatformTable {
        val createStatement = rowData.columns.joinToString(
            prefix = "CREATE TABLE $name (",
            transform = { column -> column.columnIdentifier() },
            postfix = ")"
        )
        connection.withStatement(createStatement)
        return JvmSqliteTable(connection, name, rowData)
    }

    override suspend fun dropTable(table: PlatformTable) {

    }
}

suspend fun Connection.withStatement(sql: String): Boolean {
    return withContext(Dispatchers.IO) {
        val statement = createStatement()
        val result = statement.execute(sql)
        statement.close()
        return@withContext result
    }
}