package mqtt.sql


@SQLTable(
    subclasses = [
        Child1KeyedAtParent::class, Child2KeyedAtParent::class,
        ChildWithCollectionKeyedAtParent::class, ChildWithMapKeyedAtParent::class
    ]
)
interface SupertypeKeyedAtParent {
    @PrimaryKey
    val identifier: Int
    val superParameter: CharSequence
    val property: FlatAllTypesIncludingNullableKeyedAtParent
}

@SQLTable(
    subclasses = [
        Child1KeyedAtParent::class, Child2KeyedAtParent::class,
        ChildWithCollectionKeyedAtParent::class, ChildWithMapKeyedAtParent::class
    ]
)
interface InterfaceWithGenericChildKeyedAtParent<Type : SupertypeKeyedAtParent> {
    @PrimaryKey
    val identifier: Int
    val child: Type
    val property: FlatAllTypesIncludingNullableKeyedAtParent
}

interface Child1KeyedAtParent : SupertypeKeyedAtParent {
    val propertyChild1: String?
}

interface Child2KeyedAtParent : SupertypeKeyedAtParent {
    val propertyChild2: Int
}

interface ChildWithCollectionKeyedAtParent : SupertypeKeyedAtParent {
    val propertyChild2: Collection<String>
}

interface ChildWithMapKeyedAtParent : SupertypeKeyedAtParent {
    val propertyChild2: Map<String, String>
}

@SQLTable
interface FlatAllTypesIncludingNullableKeyedAtParent {
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
