package mqtt.persistence

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(value = AndroidJUnit4::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class AndroidSimpleTests {

    @Test
    fun openTest() = runAndroidTest { contextProvider ->
        val platformDatabase = getPlatformDatabase("rahultestdb", contextProvider)
        val column1 = TextColumn("column1", "hello")
        val column2 = IntegerColumn("column2", 3)
        val column3 = FloatColumn("column3", 3.6)
        val columnsMap = mutableMapOf("column1" to column1, "column2" to column2, "column3" to column3)
        var row = Row(columnsMap)
        val map = LinkedHashMap<String, Row>()
        map["test"] = row
        val tables = platformDatabase.open(map)
        println("opened")
        val table = tables.values.first()
        val rowId = table.upsert(column1, column2, column3)
        columnsMap["rowId"] = IntegerColumn("rowId", rowId)
        row = Row(columnsMap)
        val results = table.read(rowId).associateBy { it.name }.toMutableMap()
        val resultRow = Row(results)
        assertEquals(resultRow, row)
    }
}