package mqtt.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

class JvmSqlDatabase(name: String) : PlatformDatabase {
    lateinit var connection: Connection

    override suspend fun open(tables: Map<String, Row>): Map<String, PlatformTable> {
        val connection = DriverManager.getConnection("jdbc:sqlite:")
        this.connection = connection
        val tableNames = LinkedHashSet<String>()
        val map = LinkedHashMap<String, PlatformTable>()
        withContext(Dispatchers.IO) {
            val tableResults = connection.metaData.getTables(null, null, null, arrayOf("TABLE"))
            while (tableResults.next()) {
                tableNames += tableResults.getString("TABLE")
            }
            tableResults.close()
            tableNames.forEach { tableName ->
                val columnResults = connection.metaData.getColumns(null, null, tableName, null)
                val columns = LinkedHashMap<String, Column>()
                while (columnResults.next()) {
                    val columnName = columnResults.getString("COLUMN_NAME")
                    val datatype = columnResults.getString("DATA_TYPE")
                    val columnsize = columnResults.getString("COLUMN_SIZE")
                    val decimaldigits = columnResults.getString("DECIMAL_DIGITS")
                    val isNullable = columnResults.getString("IS_NULLABLE")
                    val is_autoIncrement = columnResults.getString("IS_AUTOINCREMENT")
                    columns[columnName] = when (datatype) {
                        "VARCHAR" -> TextColumn(columnName)
                        "FLOAT" -> FloatColumn(columnName)
                        "INTEGER" -> IntegerColumn(columnName)
                        else -> BlobColumn(columnName)
                    }
                }
                columnResults.close()
                map[tableName] = JvmSqliteTable(connection, tableName, Row(columns))
            }
        }
        return map
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