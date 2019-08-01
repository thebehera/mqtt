package mqtt.sql

import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class FlatAllTypesIncludingNullableUnkeyedTests {
    private val flatTable = TableMetadata(FlatAllTypesIncludingNullableUnkeyed::class)

    @Test
    fun flatAllTypesIncludingNullableUnkeyedTableCreation() {
        val expected = """
            CREATE TABLE `mqtt.sql.FlatAllTypesIncludingNullableUnkeyed`(
              _generated_mqtt_id BIGINT NOT NULL PRIMARY KEY,
              bool BIT(1) NOT NULL,
              boolNullable BIT(1),
              byte TINYINT NOT NULL,
              byteArray BLOB NOT NULL,
              byteArrayNullable BLOB NOT NULL,
              byteNullable TINYINT,
              char CHARACTER NOT NULL,
              charNullable CHARACTER,
              double DOUBLE NOT NULL,
              doubleNullable DOUBLE,
              float FLOAT NOT NULL,
              floatNullable FLOAT,
              int INT NOT NULL,
              intNullable INT,
              long BIGINT NOT NULL,
              longNullable BIGINT,
              short SMALLINT NOT NULL,
              shortNullable SMALLINT,
              string TEXT NOT NULL,
              stringNullable TEXT,
              uByte TINYINT UNSIGNED NOT NULL,
              uByteArray BLOB NOT NULL,
              uByteArrayNullable BLOB,
              uByteNullable TINYINT UNSIGNED,
              uInt INT UNSIGNED NOT NULL,
              uIntNullable INT UNSIGNED,
              uLong BIGINT UNSIGNED NOT NULL,
              uLongNullable BIGINT UNSIGNED,
              uShort SMALLINT UNSIGNED NOT NULL,
              uShortNullable SMALLINT UNSIGNED
            );
        """.trimIndent()
        assertEquals(expected, flatTable.createTable.toString())
    }

    @Test
    fun flatAllTypesIncludingNullableUnkeyedTableCreationIfNotExists() {
        val expectedIfNotExists = """
            CREATE TABLE IF NOT EXISTS `mqtt.sql.FlatAllTypesIncludingNullableUnkeyed`(
              _generated_mqtt_id BIGINT NOT NULL PRIMARY KEY,
              bool BIT(1) NOT NULL,
              boolNullable BIT(1),
              byte TINYINT NOT NULL,
              byteArray BLOB NOT NULL,
              byteArrayNullable BLOB NOT NULL,
              byteNullable TINYINT,
              char CHARACTER NOT NULL,
              charNullable CHARACTER,
              double DOUBLE NOT NULL,
              doubleNullable DOUBLE,
              float FLOAT NOT NULL,
              floatNullable FLOAT,
              int INT NOT NULL,
              intNullable INT,
              long BIGINT NOT NULL,
              longNullable BIGINT,
              short SMALLINT NOT NULL,
              shortNullable SMALLINT,
              string TEXT NOT NULL,
              stringNullable TEXT,
              uByte TINYINT UNSIGNED NOT NULL,
              uByteArray BLOB NOT NULL,
              uByteArrayNullable BLOB,
              uByteNullable TINYINT UNSIGNED,
              uInt INT UNSIGNED NOT NULL,
              uIntNullable INT UNSIGNED,
              uLong BIGINT UNSIGNED NOT NULL,
              uLongNullable BIGINT UNSIGNED,
              uShort SMALLINT UNSIGNED NOT NULL,
              uShortNullable SMALLINT UNSIGNED
            );
        """.trimIndent()
        assertEquals(expectedIfNotExists, flatTable.createTableIfNotExists.toString())
    }

    @Test
    fun insertInto() {
        assertEquals(
            "INSERT INTO `mqtt.sql.FlatAllTypesIncludingNullableUnkeyed` VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
            flatTable.insert.statement.toString()
        )
        flatTable.insert.orderedColumns.forEachIndexed { index, tableColumn ->

        }
        flatTable.insert.orderedColumns.forEachIndexed { index, column ->

            val name = column.rawPropertyName.first().toUpperCase() + column.rawPropertyName.substring(1)
            println(
                "@Test\n" +
                        "fun propertyOrder${index}Name$name() ="
            )
            println("assertEquals(\"${column.name}\", flatTable.insert.orderedColumns[$index].name)")
            println()
            println(
                "@Test\n" +
                        "fun propertyOrder${index}BindType$name() ="
            )
            println("assertEquals(BindType.${column.bindType.name}, flatTable.insert.orderedColumns[$index].bindType)")
            println()
        }
    }

    @Test
    fun propertyOrder0Name_generated_mqtt_id() =
        assertEquals("_generated_mqtt_id", flatTable.insert.orderedColumns[0].name)

    @Test
    fun propertyOrder0BindType_generated_mqtt_id() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[0].bindType)

    @Test
    fun propertyOrder1NameBool() =
        assertEquals("bool", flatTable.insert.orderedColumns[1].name)

    @Test
    fun propertyOrder1BindTypeBool() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[1].bindType)

    @Test
    fun propertyOrder2NameBoolNullable() =
        assertEquals("boolNullable", flatTable.insert.orderedColumns[2].name)

    @Test
    fun propertyOrder2BindTypeBoolNullable() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[2].bindType)

    @Test
    fun propertyOrder3NameByte() =
        assertEquals("byte", flatTable.insert.orderedColumns[3].name)

    @Test
    fun propertyOrder3BindTypeByte() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[3].bindType)

    @Test
    fun propertyOrder4NameByteArray() =
        assertEquals("byteArray", flatTable.insert.orderedColumns[4].name)

    @Test
    fun propertyOrder4BindTypeByteArray() =
        assertEquals(BindType.Blob, flatTable.insert.orderedColumns[4].bindType)

    @Test
    fun propertyOrder5NameByteArrayNullable() =
        assertEquals("byteArrayNullable", flatTable.insert.orderedColumns[5].name)

    @Test
    fun propertyOrder5BindTypeByteArrayNullable() =
        assertEquals(BindType.Blob, flatTable.insert.orderedColumns[5].bindType)

    @Test
    fun propertyOrder6NameByteNullable() =
        assertEquals("byteNullable", flatTable.insert.orderedColumns[6].name)

    @Test
    fun propertyOrder6BindTypeByteNullable() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[6].bindType)

    @Test
    fun propertyOrder7NameChar() =
        assertEquals("char", flatTable.insert.orderedColumns[7].name)

    @Test
    fun propertyOrder7BindTypeChar() =
        assertEquals(BindType.String, flatTable.insert.orderedColumns[7].bindType)

    @Test
    fun propertyOrder8NameCharNullable() =
        assertEquals("charNullable", flatTable.insert.orderedColumns[8].name)

    @Test
    fun propertyOrder8BindTypeCharNullable() =
        assertEquals(BindType.String, flatTable.insert.orderedColumns[8].bindType)

    @Test
    fun propertyOrder9NameDouble() =
        assertEquals("double", flatTable.insert.orderedColumns[9].name)

    @Test
    fun propertyOrder9BindTypeDouble() =
        assertEquals(BindType.Double, flatTable.insert.orderedColumns[9].bindType)

    @Test
    fun propertyOrder10NameDoubleNullable() =
        assertEquals("doubleNullable", flatTable.insert.orderedColumns[10].name)

    @Test
    fun propertyOrder10BindTypeDoubleNullable() =
        assertEquals(BindType.Double, flatTable.insert.orderedColumns[10].bindType)

    @Test
    fun propertyOrder11NameFloat() =
        assertEquals("float", flatTable.insert.orderedColumns[11].name)

    @Test
    fun propertyOrder11BindTypeFloat() =
        assertEquals(BindType.Double, flatTable.insert.orderedColumns[11].bindType)

    @Test
    fun propertyOrder12NameFloatNullable() =
        assertEquals("floatNullable", flatTable.insert.orderedColumns[12].name)

    @Test
    fun propertyOrder12BindTypeFloatNullable() =
        assertEquals(BindType.Double, flatTable.insert.orderedColumns[12].bindType)

    @Test
    fun propertyOrder13NameInt() =
        assertEquals("int", flatTable.insert.orderedColumns[13].name)

    @Test
    fun propertyOrder13BindTypeInt() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[13].bindType)

    @Test
    fun propertyOrder14NameIntNullable() =
        assertEquals("intNullable", flatTable.insert.orderedColumns[14].name)

    @Test
    fun propertyOrder14BindTypeIntNullable() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[14].bindType)

    @Test
    fun propertyOrder15NameLong() =
        assertEquals("long", flatTable.insert.orderedColumns[15].name)

    @Test
    fun propertyOrder15BindTypeLong() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[15].bindType)

    @Test
    fun propertyOrder16NameLongNullable() =
        assertEquals("longNullable", flatTable.insert.orderedColumns[16].name)

    @Test
    fun propertyOrder16BindTypeLongNullable() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[16].bindType)

    @Test
    fun propertyOrder17NameShort() =
        assertEquals("short", flatTable.insert.orderedColumns[17].name)

    @Test
    fun propertyOrder17BindTypeShort() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[17].bindType)

    @Test
    fun propertyOrder18NameShortNullable() =
        assertEquals("shortNullable", flatTable.insert.orderedColumns[18].name)

    @Test
    fun propertyOrder18BindTypeShortNullable() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[18].bindType)

    @Test
    fun propertyOrder19NameString() =
        assertEquals("string", flatTable.insert.orderedColumns[19].name)

    @Test
    fun propertyOrder19BindTypeString() =
        assertEquals(BindType.String, flatTable.insert.orderedColumns[19].bindType)

    @Test
    fun propertyOrder20NameStringNullable() =
        assertEquals("stringNullable", flatTable.insert.orderedColumns[20].name)

    @Test
    fun propertyOrder20BindTypeStringNullable() =
        assertEquals(BindType.String, flatTable.insert.orderedColumns[20].bindType)

    @Test
    fun propertyOrder21NameUByte() =
        assertEquals("uByte", flatTable.insert.orderedColumns[21].name)

    @Test
    fun propertyOrder21BindTypeUByte() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[21].bindType)

    @Test
    fun propertyOrder22NameUByteArray() =
        assertEquals("uByteArray", flatTable.insert.orderedColumns[22].name)

    @Test
    fun propertyOrder22BindTypeUByteArray() =
        assertEquals(BindType.Blob, flatTable.insert.orderedColumns[22].bindType)

    @Test
    fun propertyOrder23NameUByteArrayNullable() =
        assertEquals("uByteArrayNullable", flatTable.insert.orderedColumns[23].name)

    @Test
    fun propertyOrder23BindTypeUByteArrayNullable() =
        assertEquals(BindType.Blob, flatTable.insert.orderedColumns[23].bindType)

    @Test
    fun propertyOrder24NameUByteNullable() =
        assertEquals("uByteNullable", flatTable.insert.orderedColumns[24].name)

    @Test
    fun propertyOrder24BindTypeUByteNullable() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[24].bindType)

    @Test
    fun propertyOrder25NameUInt() =
        assertEquals("uInt", flatTable.insert.orderedColumns[25].name)

    @Test
    fun propertyOrder25BindTypeUInt() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[25].bindType)

    @Test
    fun propertyOrder26NameUIntNullable() =
        assertEquals("uIntNullable", flatTable.insert.orderedColumns[26].name)

    @Test
    fun propertyOrder26BindTypeUIntNullable() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[26].bindType)

    @Test
    fun propertyOrder27NameULong() =
        assertEquals("uLong", flatTable.insert.orderedColumns[27].name)

    @Test
    fun propertyOrder27BindTypeULong() =
        assertEquals(BindType.String, flatTable.insert.orderedColumns[27].bindType)

    @Test
    fun propertyOrder28NameULongNullable() =
        assertEquals("uLongNullable", flatTable.insert.orderedColumns[28].name)

    @Test
    fun propertyOrder28BindTypeULongNullable() =
        assertEquals(BindType.String, flatTable.insert.orderedColumns[28].bindType)

    @Test
    fun propertyOrder29NameUShort() =
        assertEquals("uShort", flatTable.insert.orderedColumns[29].name)

    @Test
    fun propertyOrder29BindTypeUShort() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[29].bindType)

    @Test
    fun propertyOrder30NameUShortNullable() =
        assertEquals("uShortNullable", flatTable.insert.orderedColumns[30].name)

    @Test
    fun propertyOrder30BindTypeUShortNullable() =
        assertEquals(BindType.Long, flatTable.insert.orderedColumns[30].bindType)
}
