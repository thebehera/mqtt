package mqtt.sql

class Insert(tableMetadata: TableMetadata<*>) {
    private val insertInto by lazy {
        val orderedColumns = ArrayList<TableColumn>()
        val sql = StringBuilder("INSERT INTO ${tableMetadata.tableName} VALUES(")
        tableMetadata.columns.values.forEachIndexed { index, it ->
            orderedColumns += it
            sql.append("?")
            if (index < tableMetadata.columns.size - 1) sql.append(", ")
        }
        sql.append(");")
        Pair(sql, orderedColumns)
    }

    val statement by lazy { insertInto.first }
    val orderedColumns by lazy { insertInto.second }

}