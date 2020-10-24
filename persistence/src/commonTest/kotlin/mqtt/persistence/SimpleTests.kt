package mqtt.persistence

import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleTests {

    @Test
    fun openTest() = runTest {
        val platformDatabase = getPlatformDatabase("rahultestdb")

        val column1 = TextColumn("column1").also { it.value = "hello" }
        val column2 = IntegerColumn("column2").also { it.value = 3 }
        val row = Row(column1, column2)
        val map = LinkedHashMap<String, Row>()
        map["test"] = row
        val tables = platformDatabase.open(map)
        println("opened")
        val table = tables["test"] ?: error("Failed to find table")
        val rowId = table.upsert(column1, column2)
        val results = table.read(rowId).toTypedArray()
        println("done")
        assertEquals(Row(*results), row)
    }
}