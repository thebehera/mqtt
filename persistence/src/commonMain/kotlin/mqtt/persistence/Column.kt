package mqtt.persistence

sealed class Column(
    open val name: String,
    val type: Type,
    open val isPrimaryKey: Boolean = false,
) {
    open var value: Any? = null
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

data class IntegerColumn(override val name: String, override var value: Any? = null, override val isPrimaryKey: Boolean = false)
    : Column(name, Type.INTEGER)

data class FloatColumn(override val name: String, override var value: Any? = null,override val isPrimaryKey: Boolean = false)
    : Column(name, Type.FLOAT)

data class TextColumn(override val name: String, override var value: Any? = null, override val isPrimaryKey: Boolean = false)
    : Column(name, Type.TEXT)

data class BlobColumn(override val name: String, override var value: Any? = null, override val isPrimaryKey: Boolean = false)
    : Column(name, Type.BLOB)