package mqtt.persistence

import mqtt.buffer.PlatformBuffer

sealed class Column(
    val name: String,
    val type: Type,
    val isPrimaryKey: Boolean = false,
) {
    var value: Any? = null
    fun columnIdentifier() = if (isPrimaryKey) {
        "$name $type PRIMARY KEY"
    } else {
        "$name $type"
    }

    fun valueString() = value?.toString()

    override fun toString() = "${columnIdentifier()}\t$value"
}


fun Array<out Column>.toKeyPair(): Map<String, Any?> {
    val linkedHashMap = LinkedHashMap<String, Any?>()
    forEach { column ->
        linkedHashMap[column.name] = column.value
    }
    return linkedHashMap
}

class IntegerColumn(name: String, isPrimaryKey: Boolean = false)
    : Column(name, Type.INTEGER)

class FloatColumn(name: String, isPrimaryKey: Boolean = false)
    : Column(name, Type.FLOAT)

class TextColumn(name: String, isPrimaryKey: Boolean = false)
    : Column(name, Type.TEXT)

class BlobColumn(name: String, isPrimaryKey: Boolean = false)
    : Column(name, Type.BLOB)