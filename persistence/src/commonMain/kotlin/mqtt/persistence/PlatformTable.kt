package mqtt.persistence

interface PlatformTable {
    val name: String
    val rowData: Row
    suspend fun upsert(vararg column: Column): Long
    suspend fun read(rowId: Long) :Collection<Column>
    suspend fun delete(id: Long) {}
}