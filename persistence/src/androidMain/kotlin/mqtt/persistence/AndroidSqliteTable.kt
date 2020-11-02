package mqtt.persistence

import android.content.ContentValues
import android.database.Cursor.*
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mqtt.buffer.JvmBuffer
import java.nio.ByteBuffer

class AndroidSqliteTable(
    val database: SQLiteDatabase,
    override val rowData: Row,
    override val name: String
) : PlatformTable {

    override suspend fun upsert(vararg column: Column): Long {
        val values = ContentValues().apply {
            column.forEach { column ->
                when (column) {
                    is TextColumn -> {
                        val value = column.value
                        if (value == null) {
                            putNull(column.name)
                        } else {
                            put(column.name, value)
                        }
                    }
                    is IntegerColumn -> {
                        val value = column.value
                        if (value == null) {
                            putNull(column.name)
                        } else {
                            put(column.name, value)
                        }
                    }
                    is FloatColumn -> {
                        val value = column.value
                        if (value == null) {
                            putNull(column.name)
                        } else {
                            put(column.name, value)
                        }
                    }
                    is BlobColumn -> {
                        val value = column.value
                        if (value == null) {
                            putNull(column.name)
                        } else {
                            val byteArray = value.readByteArray(value.remaining())
                            put(column.name, byteArray)
                        }
                    }
                }
            }
        }
        return withContext(Dispatchers.IO) {
            database.replace(name, null, values)
        }
    }

    override suspend fun read(rowId: Long): Collection<Column> {
        return withContext(Dispatchers.IO) {
            database.query(
                name,
                null,
                "_rowid_ = ?",
                arrayOf(rowId.toString()),
                null,
                null,
                null
            ).use { cursor ->
                with(cursor) {
                    val list = ArrayList<Column>(cursor.columnCount)
                    (0..cursor.columnCount).forEach { index ->
                        when (getType(index)) {
                            FIELD_TYPE_NULL -> {
                            }
                            FIELD_TYPE_INTEGER -> list += IntegerColumn(getColumnName(index))
                                .also { it.value = cursor.getLong(index) }
                            FIELD_TYPE_FLOAT -> list += FloatColumn(getColumnName(index))
                                .also { it.value = cursor.getDouble(index) }
                            FIELD_TYPE_STRING -> list += TextColumn(getColumnName(index))
                                .also { it.value = cursor.getString(index) }
                            FIELD_TYPE_BLOB -> list += BlobColumn(getColumnName(index))
                                .also { it.value = JvmBuffer(ByteBuffer.wrap(cursor.getBlob(index))) }
                        }
                    }
                    list
                }
            }
        }
    }

    override suspend fun delete(rowId: Long) {
        withContext(Dispatchers.IO) {
            database.delete(name, "_rowid_ = ?", arrayOf(rowId.toString()))
        }
    }
}