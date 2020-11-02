package mqtt.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mqtt.buffer.JvmBuffer
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.Statement
import java.sql.Types.*

data class JvmSqliteTable(
    val connection: Connection,
    override val name: String, override val rowData: Row
) : PlatformTable {
    override suspend fun upsert(vararg column: Column): Long {
        val sqlPrefix = column.joinToString(
            prefix = "INSERT INTO $name (",
            postfix = ") VALUES("
        ) { it.name }
        val sql = column.joinToString(
            prefix = sqlPrefix,
            postfix = ")"
        ) { "?" }
        val statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        println(statement)
        column.forEachIndexed { index, column ->
            val actualIndex = index + 1
            when (column) {
                is IntegerColumn -> {
                    statement.setLong(actualIndex, column.value)
                }
                is FloatColumn -> {
                    statement.setDouble(actualIndex, column.value)
                }
                is TextColumn -> {
                    statement.setString(actualIndex, column.value)
                }
                is BlobColumn -> {
                    val value = column.value
                    val byteArray = value.readByteArray(value.remaining())
                    val inputStream = ByteArrayInputStream(byteArray)
                    statement.setBlob(actualIndex, inputStream, byteArray.size.toLong())
                }
                is NullColumn -> {
                    statement.setNull(actualIndex, INTEGER)
                }
            }
        }
        withContext(Dispatchers.IO) {
            statement.execute()
        }
        val resultSet = statement.generatedKeys
        val result = resultSet.getLong("last_insert_rowid()")
        resultSet.close()
        statement.close()
        return result
    }

    override suspend fun read(rowId: Long): Collection<Column> = withContext(Dispatchers.IO) {
        val statement = connection.createStatement()
        val result = statement.executeQuery("SELECT * FROM $name WHERE _rowid_ = $rowId")
        val columns = ArrayList<Column>(result.metaData.columnCount)
        (1..result.metaData.columnCount).forEach { columnIndex ->
            val columnName = result.metaData.getColumnName(columnIndex)
            columns += when (result.metaData.getColumnType(columnIndex)) {
                BIGINT, INTEGER, BOOLEAN, NUMERIC, ROWID, SMALLINT, TINYINT -> {
                    IntegerColumn(columnName, result.getLong(columnIndex))
                }
                DOUBLE, FLOAT, DECIMAL, REAL -> {
                    FloatColumn(columnName, result.getDouble(columnIndex))
                }
                VARCHAR, CHAR, LONGNVARCHAR, LONGVARCHAR, NVARCHAR -> {
                    TextColumn(columnName, result.getString(columnIndex))
                }
                BLOB -> {
                    BlobColumn(columnName, JvmBuffer(ByteBuffer.wrap(result.getBytes(columnIndex))))
                }
                else -> throw IllegalStateException("Illegal type ${result.metaData.getColumnType(columnIndex)}")
            }
        }
        if (columns.isNotEmpty()) {
            columns += IntegerColumn("rowId", rowId)
        }
        result.close()
        statement.close()
        columns
    }

    override suspend fun delete(rowId: Long) = with(Dispatchers.IO) {
        val statement = connection.createStatement()
        statement.execute("DELETE FROM $name WHERE _rowid_ = $rowId")
        statement.close()
    }
}