package mqtt.persistence

import mqtt.buffer.PlatformBuffer

sealed class Column(
    open val name: String,
    val type: Type,
    open val isPrimaryKey: Boolean = false,
) {
    open val value: Any? = null
    fun columnIdentifier() = if (isPrimaryKey) {
        "$name $type PRIMARY KEY"
    } else {
        "$name $type"
    }

}

fun Array<out Column>.toKeyPair(): Map<String, Any?> {
    val linkedHashMap = LinkedHashMap<String, Any?>()
    forEach { column ->
        linkedHashMap[column.name] = column.value
    }
    return linkedHashMap
}

data class IntegerColumn(
    override val name: String,
    override val value: Long,
    override val isPrimaryKey: Boolean = false
) : Column(name, Type.INTEGER)

data class FloatColumn(
    override val name: String,
    override val value: Double,
    override val isPrimaryKey: Boolean = false
) : Column(name, Type.REAL)

data class TextColumn(
    override val name: String,
    override val value: String,
    override val isPrimaryKey: Boolean = false
) : Column(name, Type.TEXT)

data class BlobColumn(
    override val name: String,
    override val value: PlatformBuffer,
    override val isPrimaryKey: Boolean = false
) : Column(name, Type.BLOB)

data class NullColumn(override val name: String, override val isPrimaryKey: Boolean = false) : Column(name, Type.NULL) {
    override val value: Any? = null
}