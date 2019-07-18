package mqtt.sql

import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

class JVMTests {
    @SQLTable
    interface B {
        @Meow
        @PrimaryKey
        val c: String
    }

    @SQLTable
    class XB(
        @Meow override val c: String = "c",
        @Meow
        val bool: Boolean,
        @Transient
        val byte: Byte,
        val uByte: UByte,
        val short: Short,
        val uShort: UShort,
        val int32: Int,
        val uint32: UInt,
        val long: Long,
        val ulong: ULong,
        val charac: Char,
        val float: Float,
        val double: Double,
        val string: String,
        val byteArray: ByteArray,
        val uByteArray: UByteArray,
        val javaBigInt: BigInteger,
        val bigDecimal: BigDecimal,
        val date: Date?,
        val sqlDate: java.sql.Date,
        val cal: Calendar,
        val from: String,
        val gregorianCalendar: GregorianCalendar,
        val byteBuffer: ByteBuffer
    ) : B

    @Test
    fun bigIntTest() {
        val xb: KClass<XB> = XB::class
        println(xb.getAnnotationsIncludingSuperclass())
        println(createTable(XB::class) {
            val nonNullReturnType = it.returnType.withNullability(false)
            when {
                nonNullReturnType.isSubtypeOf(BigInteger::class.starProjectedType.withNullability(false)) ->
                    return@createTable "TEXT"
                nonNullReturnType.isSubtypeOf(BigDecimal::class.starProjectedType.withNullability(false)) ->
                    return@createTable "TEXT"
                nonNullReturnType.isSubtypeOf(Date::class.starProjectedType.withNullability(false)) ->
                    return@createTable "DATETIME"
                nonNullReturnType.isSubtypeOf(Calendar::class.starProjectedType.withNullability(false)) ->
                    return@createTable "DATETIME"
                nonNullReturnType.isSubtypeOf(ByteBuffer::class.starProjectedType.withNullability(false)) ->
                    return@createTable "BLOB"
                else ->
                    return@createTable null
            }
        })
        println(insertInto<XB>())
    }
}