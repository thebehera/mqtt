package mqtt.persistence

import android.content.ContentValues
import android.database.Cursor.*
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mqtt.buffer.JvmBuffer
import java.nio.ByteBuffer

class AndroidSqliteTable(
    val database: SQLiteOpenHelper,
    override val rowData: Row,
    override val name: String
) : PlatformTable {

    override suspend fun upsert(vararg column: Column): Long {
        val values = ContentValues().apply {
            column.forEach { column ->
                when (column) {
                    is TextColumn -> {
                        put(column.name, column.value)
                    }
                    is IntegerColumn -> {
                        put(column.name, column.value)
                    }
                    is FloatColumn -> {
                        put(column.name, column.value)
                    }
                    is BlobColumn -> {
                        val value = column.value
                        val byteArray = value.readByteArray(value.remaining())
                        put(column.name, byteArray)
                    }
                    is NullColumn -> putNull(column.name)
                }
            }
        }
        return withContext(Dispatchers.IO) {
            database.writableDatabase.replace(name, null, values)
        }
    }

    override suspend fun read(rowId: Long): Collection<Column> {
        return withContext(Dispatchers.IO) {
            database.readableDatabase.query(
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
                            FIELD_TYPE_NULL -> list += NullColumn(getColumnName(index))
                            FIELD_TYPE_INTEGER -> list += IntegerColumn(getColumnName(index), cursor.getLong(index))
                            FIELD_TYPE_FLOAT -> list += FloatColumn(getColumnName(index), cursor.getDouble(index))
                            FIELD_TYPE_STRING -> list += TextColumn(getColumnName(index), cursor.getString(index))
                            FIELD_TYPE_BLOB -> list += BlobColumn(
                                getColumnName(index),
                                JvmBuffer(ByteBuffer.wrap(cursor.getBlob(index)))
                            )
                        }
                    }
                    list
                }
            }
        }
    }

    override suspend fun delete(rowId: Long) {
        withContext(Dispatchers.IO) {
            database.writableDatabase.delete(name, "_rowid_ = ?", arrayOf(rowId.toString()))
        }
    }
}