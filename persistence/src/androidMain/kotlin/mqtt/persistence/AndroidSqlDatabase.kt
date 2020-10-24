package mqtt.persistence

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidSqlDatabase(name: String): PlatformDatabase {
    lateinit var database: SQLiteDatabase
    override suspend fun  createTable(name: String, rowData: Row): PlatformTable{
        val statement = rowData.columns.joinToString(
            prefix = "CREATE TABLE $name (",
            transform = {column -> column.columnIdentifier()},
            postfix = ")"
        )
        withContext(Dispatchers.IO) {
            database.execSQL(statement)
        }
        return AndroidSqliteTable(database, rowData, name)
    }

    override suspend fun dropTable(table: PlatformTable) {

    }
}