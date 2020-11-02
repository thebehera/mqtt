package mqtt.persistence

import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleTests {

    @Test
    fun openTest() = runTest {
        val platformDatabase = getPlatformDatabase("rahultestdb")

        val column1 = TextColumn("column1", "hello")
        val column2 = IntegerColumn("column2", 3)
        val column3 = FloatColumn("column3", 3.6)
        val columnsMap = mutableMapOf("column1" to column1, "column2" to column2, "column3" to column3)
        var row = Row(columnsMap)
        val map = LinkedHashMap<String, Row>()
        map["test"] = row
        println("tables")
        val tables = platformDatabase.open(map)
        val table = tables.values.first()
        val rowId = table.upsert(column1, column2, column3)
        columnsMap["rowId"] = IntegerColumn("rowId", rowId)
        row = Row(columnsMap)
        val results = table.read(rowId).associateBy { it.name }.toMutableMap()
        val resultRow = Row(results)
        println(" $resultRow\r\n")
        println(" $row\n")
        assertEquals(resultRow, row)
        return@runTest
    }
}