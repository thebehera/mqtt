@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.sql

//fun main() {
//    println(createTableInheritence<>())
//}
//
//@SQLTable(
//    subclasses = [
//        Child1Unkeyed::class, Child2Unkeyed::class,
//        ChildWithCollectionUnkeyed::class, ChildWithMapUnkeyed::class
//    ]
//)
//interface SupertypeUnkeyed {
//    val superParameter: CharSequence
//    val property: FlatAllTypesIncludingNullableUnkeyed
//}
//
//@SQLTable(
//    subclasses = [
//        Child1Unkeyed::class, Child2Unkeyed::class,
//        ChildWithCollectionUnkeyed::class, ChildWithMapUnkeyed::class
//    ]
//)
//interface InterfaceWithGenericChildUnkeyed<Type : SupertypeUnkeyed> {
//    val child: Type
//    val property: FlatAllTypesIncludingNullableUnkeyed
//}
//
//interface Child1Unkeyed : SupertypeUnkeyed {
//    val propertyChild1: String?
//}
//
//interface Child2Unkeyed : SupertypeUnkeyed {
//    val propertyChild2: Int
//}
//
//interface ChildWithCollectionUnkeyed : SupertypeUnkeyed {
//    val propertyChild2: Collection<String>
//}
//
//interface ChildWithMapUnkeyed : SupertypeUnkeyed {
//    val propertyChild2: Map<String, String>
//}
//
//@SQLTable
//interface FlatAllTypesIncludingNullableUnkeyed {
//    val bool: Boolean
//    val byte: Byte
//    val uByte: UByte
//    val short: Short
//    val uShort: UShort
//    val int: Int
//    val uInt: UInt
//    val long: Long
//    val uLong: ULong
//    val char: Char
//    val float: Float
//    val double: Double
//    val string: String
//    val byteArray: ByteArray
//    val uByteArray: UByteArray
//    val boolNullable: Boolean?
//    val byteNullable: Byte?
//    val uByteNullable: UByte?
//    val shortNullable: Short?
//    val uShortNullable: UShort?
//    val intNullable: Int?
//    val uIntNullable: UInt?
//    val longNullable: Long?
//    val uLongNullable: ULong?
//    val charNullable: Char?
//    val floatNullable: Float?
//    val doubleNullable: Double?
//    val stringNullable: String?
//    val byteArrayNullable: ByteArray
//    val uByteArrayNullable: UByteArray?
//}
