package mqtt.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mqtt.buffer.JvmBuffer
import java.io.ByteArrayInputStream
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.Statement
import java.sql.Types

class JvmSqliteTable(val connection: Connection,
                     override val name: String, override val rowData: Row)
    : PlatformTable {
    override suspend fun upsert(vararg columns: Column): Long {
        val sqlPrefix = columns.joinToString(
            prefix = "UPSERT INTO $name(",
            postfix = ") VALUES(") { it.name }
        val sql = columns.joinToString(
            prefix = sqlPrefix,
            postfix = ")"
        ) {"?"}

            val statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
            columns.forEachIndexed { index, column ->
                val actualIndex = index + 1
                when (column) {
                    is IntegerColumn -> {
                        val value = column.value
                        if (value == null) {
                            statement.setNull(actualIndex, Types.BIGINT)
                        } else {
                            statement.setLong(actualIndex, value)
                        }
                    }
                    is FloatColumn -> {
                        val value = column.value
                        if (value == null) {
                            statement.setNull(actualIndex, Types.DOUBLE)
                        } else {
                            statement.setDouble(actualIndex, value)
                        }
                    }
                    is TextColumn -> {
                        val value = column.value
                        if (value == null) {
                            statement.setNull(actualIndex, Types.VARCHAR)
                        } else {
                            statement.setString(actualIndex, value)
                        }
                    }
                    is BlobColumn -> {
                        val value = column.value
                        if (value == null) {
                            statement.setNull(actualIndex, Types.BLOB)
                        } else {
                            val byteArray = value.readByteArray(value.remaining())
                            val inputStream = ByteArrayInputStream(byteArray)
                            statement.setBlob(actualIndex, inputStream, byteArray.size.toLong())
                        }
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
        (1..result.metaData.columnCount + 1).forEach { columnIndex ->
            columns += when (result.metaData.getColumnType(columnIndex)) {
                Types.BIGINT -> {
                    IntegerColumn(result.metaData.getColumnName(columnIndex))
                        .also { it.value = result.getLong(columnIndex) }
                }
                Types.DOUBLE -> {
                    FloatColumn(result.metaData.getColumnName(columnIndex))
                        .also { it.value = result.getDouble(columnIndex) }
                }
                Types.VARCHAR -> {
                    TextColumn(result.metaData.getColumnName(columnIndex))
                        .also { it.value = result.getString(columnIndex) }
                }
                Types.BLOB -> {
                    BlobColumn(result.metaData.getColumnName(columnIndex))
                        .also { it.value = JvmBuffer(ByteBuffer.wrap(result.getBytes(columnIndex))) }
                }
                else -> throw IllegalStateException("Illegal type ${result.metaData.getColumnType(columnIndex)}")
            }
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