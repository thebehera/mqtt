package mqtt.sql

@SQLTable(
    subclasses = [
        Child1KeyedAtChild::class, Child2KeyedAtChild::class,
        ChildWithCollectionKeyedAtChild::class, ChildWithMapKeyedAtChild::class
    ]
)
interface SupertypeKeyedAtChild {
    val superParameter: CharSequence
    val property: FlatAllTypesIncludingNullableKeyedAtChild
}

@SQLTable(
    subclasses = [
        Child1KeyedAtChild::class, Child2KeyedAtChild::class,
        ChildWithCollectionKeyedAtChild::class, ChildWithMapKeyedAtChild::class
    ]
)
interface InterfaceWithGenericChildKeyedAtChild<Type : SupertypeKeyedAtChild> {
    val child: Type
    val property: FlatAllTypesIncludingNullableKeyedAtChild
}

interface Child1KeyedAtChild : SupertypeKeyedAtChild {
    @PrimaryKey
    val propertyChild1: String?
}

interface Child2KeyedAtChild : SupertypeKeyedAtChild {
    @PrimaryKey
    val propertyChild2: Int
}

interface ChildWithCollectionKeyedAtChild : SupertypeKeyedAtChild {
    @PrimaryKey
    val key: String
    val propertyChild2: Collection<String>
}

interface ChildWithMapKeyedAtChild : SupertypeKeyedAtChild {
    @PrimaryKey
    val key: Int
    val propertyChild2: Map<String, String>
}

@SQLTable
interface FlatAllTypesIncludingNullableKeyedAtChild {
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
    @PrimaryKey
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