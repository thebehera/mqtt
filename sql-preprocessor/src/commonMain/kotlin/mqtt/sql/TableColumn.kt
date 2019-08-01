@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.sql

import kotlin.reflect.KProperty

data class TableColumn(
    val parent: TableMetadata<*>,
    val property: KProperty<*>,
    val customClasses: (KProperty<*>) -> Pair<CharSequence, BindType>? = { null },
    val nativeClasses: (KProperty<*>) -> Pair<CharSequence, BindType>? = { null },
    val spacer: CharSequence = parent.spacer
) {

    private val returnType by lazy {
        val classifier = property.returnType.classifier!!
        customClasses(property)?.first ?: when (classifier) {
            Boolean::class -> "BIT(1)"
            Byte::class -> "TINYINT"
            UByte::class -> "TINYINT UNSIGNED"
            Short::class -> "SMALLINT"
            UShort::class -> "SMALLINT UNSIGNED"
            Int::class -> "INT"
            UInt::class -> "INT UNSIGNED"
            Long::class -> "BIGINT"
            ULong::class -> "BIGINT UNSIGNED"
            Char::class -> "CHARACTER"
            Float::class -> "FLOAT"
            Double::class -> "DOUBLE"
            Number::class, // This could be a BigInteger that isn't supported by implementations of SQL
            String::class, CharSequence::class, StringBuilder::class -> "TEXT"
            ByteArray::class, UByteArray::class,
            ShortArray::class, UShortArray::class,
            IntArray::class, UIntArray::class,
            LongArray::class, ULongArray::class -> "BLOB"
            else -> nativeClasses(property)?.first ?: property.returnType.toString()
        }
    }

    val bindType by lazy {
        val classifier = property.returnType.classifier!!
        customClasses(property)?.second ?: when (classifier) {
            Boolean::class,
            Byte::class, UByte::class,
            Short::class, UShort::class,
            Int::class, UInt::class,
            Long::class -> BindType.Long
            Float::class,
            Double::class -> BindType.Double
            Char::class,
            Number::class,
            ULong::class,
            String::class, CharSequence::class, StringBuilder::class -> BindType.String
            ByteArray::class, UByteArray::class,
            ShortArray::class, UShortArray::class,
            IntArray::class, UIntArray::class,
            LongArray::class, ULongArray::class -> BindType.Blob
            else -> nativeClasses(property)?.second ?: BindType.String
        }
    }

    val annotations = parent.nameAnnotationsMap[property.name] ?: emptyList<Annotation>()
    val isNullable = property.returnType.isMarkedNullable
    val primaryKeyAnnotation = annotations.findInstanceOf<PrimaryKey>()
    val hasPrimaryKey = primaryKeyAnnotation != null
    val uniqueAnnotation = annotations.findInstanceOf<Unique>()
    val isUnique = uniqueAnnotation != null
    val checkAnnotation = annotations.findInstanceOf<Check>()
    val hasCheck = checkAnnotation != null
    val foreignKey = annotations.findInstanceOf<ForeignKey>()
    val hasForeignKey = foreignKey != null
    val rawPropertyName = property.name
    val name by lazy { rawPropertyName.escapeNameIfNeeded() }


    fun create(): CharSequence {
        val sql = StringBuilder()
        sql.append("$spacer $name $returnType")
        if (!isNullable) {
            if (!sql.endsWith(spacer)) {
                sql.append(spacer)
            }
            sql.append("NOT NULL")
        }
        if (hasPrimaryKey) {
            if (!sql.endsWith(spacer)) {
                sql.append(spacer)
            }
            sql.append("PRIMARY KEY")
        }
        if (isUnique) {
            if (!sql.endsWith(spacer)) {
                sql.append(spacer)
            }
            sql.append("UNIQUE")
        }
        if (hasCheck) {
            if (!sql.endsWith(spacer)) {
                sql.append(' ')
            }
            sql.append("CHECK(${checkAnnotation!!.expression})")
        }
        return sql
    }


}