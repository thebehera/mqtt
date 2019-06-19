package mqtt

import mqtt.sql.SQLTable
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleTest {
    @Test
    fun additionWorks() = assertEquals(4, 2 + 2)

    @Test
    fun ensureSimpleTableCreation() {
        @SQLTable
        data class Yolo(val x: String = "x")

    }
}