package mqtt.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mqtt.buffer.allocateNewBuffer
import java.sql.Connection
import java.sql.DriverManager

class JvmSqlDatabase(val name: String) : PlatformDatabase {
    lateinit var connection: Connection

    override suspend fun open(tables: Map<String, Row>): Map<String, PlatformTable> {
        val connection = withContext(Dispatchers.IO) { DriverManager.getConnection("jdbc:sqlite:$name.sqlite") }
        this.connection = connection
        val tablesRead = getTables()
        val copiedExpectedTables = LinkedHashMap(tables)
        for (table in tablesRead) {
            copiedExpectedTables.remove(table.key)
        }
        if (copiedExpectedTables.isNotEmpty()) {
            for (table in copiedExpectedTables) {
                println("creating table ${table.key}")
                createTable(table.key, table.value)
            }
            val tables = getTables()
            println(tables)
            return tables
        }
        println(tablesRead)
        return tablesRead
    }

    private suspend fun getTables(): Map<String, PlatformTable> {
        val map = LinkedHashMap<String, PlatformTable>()
        val tableNames = LinkedHashSet<String>()
        withContext(Dispatchers.IO) {
            val tableResults = connection.metaData.getTables(null, null, null, arrayOf("TABLE"))
            while (tableResults.next()) {
                tableNames += tableResults.getString("TABLE_NAME")
            }
            tableResults.close()
            tableNames.forEach { tableName ->
                val columnResults = connection.metaData.getColumns(null, null, tableName, null)
                val columns = LinkedHashMap<String, Column>()
                while (columnResults.next()) {
                    val columnName = columnResults.getString("COLUMN_NAME")
                    val datatype = columnResults.getString("TYPE_NAME")
                    val columnsize = columnResults.getString("COLUMN_SIZE")
                    val decimaldigits = columnResults.getString("DECIMAL_DIGITS")
                    val isNullable = columnResults.getString("IS_NULLABLE")
                    val is_autoIncrement = columnResults.getString("IS_AUTOINCREMENT")
                    columns[columnName] = when (datatype) {
                        "TEXT" -> TextColumn(columnName, "")
                        "REAL" -> FloatColumn(columnName, 0.0)
                        "NULL" -> NullColumn(columnName)
                        "INTEGER" -> IntegerColumn(columnName, 0)
                        else -> BlobColumn(columnName, allocateNewBuffer(1u))
                    }
                }
                columnResults.close()
                map[tableName] = JvmSqliteTable(connection, tableName, Row(columns))
            }
        }
        return map
    }

    suspend fun createTable(name: String, rowData: Row): PlatformTable {
        val createStatement = rowData.columns.values.joinToString(
            prefix = "CREATE TABLE $name (",
            transform = { column -> column.columnIdentifier() },
            postfix = ")"
        )
        println("$createStatement ${connection.withStatement(createStatement)}")

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