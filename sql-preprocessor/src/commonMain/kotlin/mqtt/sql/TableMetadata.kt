package mqtt.sql

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class TableMetadata<T : Any>(
    val type: KClass<T>,
    val customClasses: (KProperty<*>) -> Pair<CharSequence, BindType>? = { null },
    val nativeClasses: (KProperty<*>) -> Pair<CharSequence, BindType>? = { null },
    val spacer: CharSequence = " ",
    val lineBreak: CharSequence = "\n"
) {

    val members by lazy { type.members.filterIsInstance<KProperty<*>>() }

    val nameAnnotationsMap = type.getAnnotationsIncludingSuperclass().toMutableMap()

    val hasPrimaryKey by lazy {
        var hasKey = false
        for (tableColumn in columns.values) {
            if (tableColumn.hasPrimaryKey) {
                hasKey = true
                break
            }
        }
        hasKey
    }

    val hasForeignKey by lazy {
        var hasKey = false
        for (tableColumn in columns.values) {
            if (tableColumn.hasForeignKey) {
                hasKey = true
                break
            }
        }
        hasKey
    }

    val rawTableName by lazy {
        val annotationsOnClass = nameAnnotationsMap[""]
        var tableNameCandidate = type.qualifiedName!!
        if (annotationsOnClass != null) {
            for (annotation in annotationsOnClass) {
                if (annotation is SQLTable) {
                    if (annotation.tableName.isNotEmpty()) {
                        tableNameCandidate = annotation.tableName
                    }
                }
            }
        }
        tableNameCandidate
    }
    val tableName by lazy { rawTableName.escapeNameIfNeeded() }

    val columns by lazy {
        val columns = LinkedHashMap<String, TableColumn>(members.size)
        nameAnnotationsMap["_generated_mqtt_id"] =
            listOf(LongWrapper::class.getMemberAnnotations().values.first().first())

        val tableColumn = TableColumn(this, LongWrapper::class.members.first() as KProperty<*>)
        columns[tableColumn.name] = tableColumn
        var hasAKey = false
        members.forEach {
            val column = TableColumn(this, it, customClasses, nativeClasses, spacer)
            columns[it.name] = column
            if (column.hasForeignKey || column.hasPrimaryKey) {
                hasAKey = true
            }
        }
        if (hasAKey) {
            columns.remove(tableColumn.name)
        } else {

        }
        columns
    }

    val createTableIfNotExists by lazy { createTable(true) }
    val createTable by lazy { createTable(false) }

    private fun createTable(ifNotExists: Boolean): CharSequence {
        val sql = StringBuilder("CREATE TABLE ")
        if (ifNotExists) {
            sql.append("IF NOT EXISTS ")
        }
        sql.append("$tableName($lineBreak")
        val lastColumn = columns.values.last()
        columns.values.forEach {
            sql.append(it.create())
            sql.append(
                if (it == lastColumn) {
                    "$lineBreak"
                } else {
                    ",$lineBreak"
                }
            )
        }

        val lastKey = foreignKeyMap.keys.lastOrNull()
        for ((childColumn, parent) in foreignKeyMap) {
            sql.append("${spacer}FOREIGN KEY($childColumn) REFERENCES `${parent.table.qualifiedName}`(${parent.column})")
            if (parent.onDelete != ForeignKeyActions.NoAction) {
                sql.append(" ON DELETE ${parent.onDelete.action}")
            }
            if (parent.onUpdate != ForeignKeyActions.NoAction) {
                sql.append(" ON UPDATE ${parent.onUpdate.action}")
            }
            val isLast = childColumn == lastKey
            if (isLast) {
                sql.append('\n')
            } else {
                sql.append(",$lineBreak")
            }
        }
        sql.append(");")
        return sql
    }


    val foreignKeyMap by lazy {
        val foreignKeys = LinkedHashMap<TableColumn, ForeignKey>()
        members.forEach {
            val annotations = nameAnnotationsMap[it.name] ?: emptyList()
            val foreignKey = annotations.findInstanceOf<ForeignKey>()
            if (foreignKey is ForeignKey) {
                val tableColumn = columns[it.name]!!
                foreignKeys[tableColumn] = foreignKey
            }
        }
        foreignKeys
    }


    val insert by lazy { Insert(this) }

    fun createView(parentColumn: TableColumn, parentSortChild: String, childColumn: TableColumn, limit: Int = 1)
            : CharSequence {
        val childTableName = childColumn.parent.tableName
        val viewName = "`${tableName}_$childTableName`"
        return """
        CREATE VIEW $viewName AS 
        SELECT `$tableName`.$parentColumn, `$childTableName`.*
        FROM `$tableName`
        INNER JOIN `$childTableName` ON `$tableName`.$parentColumn = `$childTableName`.${childColumn.name}
        ORDER BY `$tableName`.$parentSortChild
        LIMIT $limit;
    """.trimIndent()
    }

    fun createChildDeleteTrigger(parentColumn: TableColumn, childColumn: TableColumn): CharSequence {
        val childTableName = childColumn.parent.tableName
        return """
        CREATE TRIGGER `$childTableName.${childColumn.rawPropertyName}_DELETE_FROM_QUEUED` BEFORE DELETE ON `$childTableName`
        BEGIN
         DELETE FROM $tableName WHERE ${parentColumn.name} = OLD.${childColumn.name};
        END;
    """.trimIndent()
    }

    fun createChildMappingDeleteTrigger(parentColumn: TableColumn, children: Collection<TableColumn>)
            : List<CharSequence> {
        val childDeleteTriggers = ArrayList<CharSequence>(children.size)
        children.forEach {
            childDeleteTriggers += createChildDeleteTrigger(parentColumn, it)
        }
        return childDeleteTriggers
    }

    fun delete(column: TableColumn) = "DELETE FROM '$tableName' WHERE ${column.name} = ?;"

    inline fun <reified A : Annotation> findColumnsForAnnotation(): Collection<TableColumn> {
        val set = HashSet<TableColumn>()
        columns.values.forEach { column ->
            column.annotations.forEach { annotation ->
                if (A::class.isInstance(annotation)) {
                    set += column
                }
            }
        }
        return set
    }

    fun createChildTablesForInheritence(): List<CharSequence> {
        val sqlTable = type.annotations.findInstanceOf<SQLTable>() ?: return emptyList()
        val childTables = subclasses(sqlTable)

        // +1 for parent
        val tables = ArrayList<CharSequence>(childTables.size + 1)
        tables += createTable
        val primaryKey: TableColumn? = findColumnsForAnnotation<PrimaryKey>().firstOrNull()
        val children = LinkedHashSet<TableColumn>()

        val deletes = LinkedHashMap<KClass<*>, String>(childTables.size)
        if (primaryKey != null) {
            deletes[type] = delete(primaryKey)
        }
        childTables.forEach {
            tables += it.createTable
            if (primaryKey != null) {
                val foreignKey = it.findColumnsForAnnotation<ForeignKey>().firstOrNull()
                if (foreignKey != null) {
                    children += foreignKey
                    deletes[it.type] = it.delete(foreignKey)
                }
            } else {

            }
        }
        if (primaryKey != null) {
            deletes.values.forEach {
                tables += it
            }
            createChildMappingDeleteTrigger(primaryKey, children).forEach {
                tables += it
            }
        }
        return tables
    }

    private fun subclasses(sqlTable: SQLTable): Collection<TableMetadata<*>> {
        val set = HashSet<TableMetadata<*>>(sqlTable.subclasses.size)
        sqlTable.subclasses.forEach {
            set += TableMetadata(it, customClasses, nativeClasses, spacer, lineBreak)
        }
        return set
    }

    companion object {
        data class LongWrapper(@PrimaryKey val _generated_mqtt_id: Long)
    }
}