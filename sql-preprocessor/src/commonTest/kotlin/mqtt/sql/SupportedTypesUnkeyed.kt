@file:Suppress("EXPERIMENTAL_API_USAGE")
package mqtt.sql

@SQLTable
interface FlatAllTypesIncludingNullableUnkeyed {
    val bool: Boolean
    val byte: Byte
    val uByte: UByte
    val short: Short
    val uShort: UShort
    val int: Int
    val uInt: UInt
    val long: Long
    val uLong: ULong
    val char: Char
    val float: Float
    val double: Double
    val string: String
    val byteArray: ByteArray
    val uByteArray: UByteArray
    val boolNullable: Boolean?
    val byteNullable: Byte?
    val uByteNullable: UByte?
    val shortNullable: Short?
    val uShortNullable: UShort?
    val intNullable: Int?
    val uIntNullable: UInt?
    val longNullable: Long?
    val uLongNullable: ULong?
    val charNullable: Char?
    val floatNullable: Float?
    val doubleNullable: Double?
    val stringNullable: String?
    val byteArrayNullable: ByteArray
    val uByteArrayNullable: UByteArray?
}