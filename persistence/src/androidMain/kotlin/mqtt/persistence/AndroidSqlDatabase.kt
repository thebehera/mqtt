package mqtt.persistence

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidSqlDatabase(val name: String, val contextProvider: AndroidContextProvider) : PlatformDatabase {
    lateinit var dbHelper: SQLiteOpenHelper
    override suspend fun open(tables: Map<String, Row>): Map<String, PlatformTable> {
        val tablesFound = LinkedHashMap<String, PlatformTable>()

        suspendCoroutine<Unit> {
            val helper = object : SQLiteOpenHelper(contextProvider.context, "$name.db", null, 1) {
                override fun onCreate(db: SQLiteDatabase) {
                    tables.keys.forEach { tableName ->
                        tablesFound[tableName] = createTableBlocking(db, tableName, tables[tableName]!!)
                    }
                }

                override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

                }

                override fun onOpen(db: SQLiteDatabase) {
//                    db.toString()
//                    db.rawQuery(
//                        "SELECT name FROM sqlite_master WHERE type='table' AND name!='android_metadata' order by name",
//                        null
//                    ).use { tableCursor ->
//                        val tableName = tableCursor.getString(0)
//                        val rowData = LinkedHashMap<String, Column>(tableCursor.columnCount)
//                        (0 until tableCursor.columnCount).forEach { index ->
//                            val columnName = tableCursor.getColumnName(index)
//                            when (tableCursor.getType(index)) {
//                                FIELD_TYPE_NULL -> rowData[columnName] = NullColumn(columnName)
//                                FIELD_TYPE_INTEGER -> rowData[columnName] = IntegerColumn(columnName, 0)
//                                FIELD_TYPE_FLOAT -> rowData[columnName] = FloatColumn(columnName, 0.0)
//                                FIELD_TYPE_STRING -> rowData[columnName] = TextColumn(columnName, "")
//                                FIELD_TYPE_BLOB -> rowData[columnName] = BlobColumn(
//                                    tableCursor.getColumnName(index), allocateNewBuffer(
//                                        1u
//                                    )
//                                )
//                            }
//                        }
//                        tablesFound[tableName] = AndroidSqliteTable(this, Row(rowData), name)
//                    }
                    tablesFound.putAll(getDatabaseStructure(db))
                    it.resume(Unit)
                }
            }
            this.dbHelper = helper
            helper.readableDatabase
        }
        this.dbHelper = dbHelper
        return tablesFound
    }

    fun getDatabaseStructure(db: SQLiteDatabase): Map<String, PlatformTable> {
        val map = LinkedHashMap<String, PlatformTable>()
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table'", null
        ).use { sqliteMasterCursor ->
            var i = 0
            sqliteMasterCursor.moveToFirst()
            while (!sqliteMasterCursor.isAfterLast) {
                i = 0
                while (i < sqliteMasterCursor.columnCount) {
                    val tableName = sqliteMasterCursor.getString(i++)
                    if (tableName == "android_metadata") {
                        continue
                    }
                    val columns = mutableMapOf<String, Column>()
                    db.rawQuery(
                        "SELECT * FROM $tableName", null
                    ).use { columnCursor ->
                        columnCursor.moveToFirst()
                        val columnNames = columnCursor.columnNames
                        for (columnIndex in columnNames.indices) {
                            columnCursor.move(columnIndex)
                            columns[columnNames[columnIndex]] = IntegerColumn(columnNames[columnIndex], 0)
                        }
                    }
                    map[tableName] = AndroidSqliteTable(dbHelper, Row(columns), tableName)
                }
                sqliteMasterCursor.moveToNext()
            }

        }
        return map
    }

    fun createTableBlocking(db: SQLiteDatabase, name: String, rowData: Row): PlatformTable {
        val statement = rowData.columns.values.joinToString(
            prefix = "CREATE TABLE $name (",
            transform = { column -> column.columnIdentifier() },
            postfix = ")"
        )
        println("${db.execSQL(statement)} $statement")
        return AndroidSqliteTable(dbHelper, rowData, name)
    }

    override suspend fun dropTable(table: PlatformTable) {

    }
}