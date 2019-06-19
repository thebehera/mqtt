package mqtt.sql

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@ExperimentalUnsignedTypes
inline fun <reified T> createTable(
    customClasses: (KProperty<*>) -> String? = { null },
    nativeClasses: (KProperty<*>) -> String? = { null }
): String {
    val members = T::class.members
        .filter { it is KProperty }

    val annotationsMap = T::class.getAnnotationsIncludingSuperclass()

    val annotatationsOnClass = annotationsMap[""]
    var tableName = T::class.qualifiedName
    if (annotatationsOnClass != null) {
        for (annotation in annotatationsOnClass) {
            if (annotation is SQLTable) {
                if (annotation.tableName.isNotEmpty()) {
                    tableName = annotation.tableName
                }
            }
        }
    }
    val sql = StringBuilder("CREATE TABLE '$tableName'(\n")

    members.forEachIndexed { index, it ->
        val property = it as KProperty<*>
        val classifier = property.returnType.classifier!!
        val returnType = customClasses(property) ?: when (classifier) {
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
            String::class -> "TEXT"
            ByteArray::class, UByteArray::class -> "BLOB"
            else -> nativeClasses(property) ?: property.returnType.toString() // BLOB AKA NONE
        }
        if (SQLITE_KEY_WORDS.contains(it.name.toUpperCase())) {
            sql.append(" `${it.name}` $returnType")
        } else {
            sql.append(" ${it.name} $returnType")
        }
        val annotations = annotationsMap[it.name] ?: emptyList()
        val isPrimaryKey = annotations.findInstanceOf<PrimaryKey>() != null
        if (!it.returnType.isMarkedNullable) {
            if (sql[sql.lastIndex] != ' ') {
                sql.append(' ')
            }
            sql.append("NOT NULL")
        }
        if (isPrimaryKey) {
            if (sql[sql.lastIndex] != ' ') {
                sql.append(' ')
            }
            sql.append("PRIMARY KEY")
        }
        if (index < members.size - 1) {
            sql.append(",\n")
        } else {
            sql.append("\n")
        }
    }
    sql.append(");")
    return sql.toString()
}

inline fun <reified T> insertInto(tableName: String? = T::class.qualifiedName): String {
    val sql = StringBuilder("INSERT INTO '$tableName' VALUES(")
    val members = T::class.members
        .filter { it is KProperty }
    members.forEachIndexed { index, it ->
        val wrapInSingleQuote = it.returnType.classifier == String::class
        if (wrapInSingleQuote) sql.append('\'')
        sql.append("\${${it.name}}")
        if (wrapInSingleQuote) sql.append('\'')
        if (index < members.size - 1) sql.append(", ")
    }
    sql.append(");")
    return sql.toString()
}

inline fun <I, reified T> selectData(tableName: String? = T::class.qualifiedName, identifier: I) =
    "SELECT * FROM '$tableName'"

inline fun <reified T> List<Annotation>.findInstanceOf(): Annotation? {
    for (annotation in this) {
        if (annotation is T) {
            return annotation
        }
    }
    return null
}

fun KClass<*>.getAnnotationsIncludingSuperclass(): Map<String, List<Annotation>> {
    val map = getMemberAnnotations()
    for (it in supertypes) {
        if (it.classifier == Any::class) {
            continue
        }
        val superMap = (it.classifier as KClass<*>).getAnnotationsIncludingSuperclass()
        map.safeAdd(superMap)
    }
    return map
}

fun MutableMap<String, MutableList<Annotation>>.safeAdd(other: Map<String, List<Annotation>>) {
    for ((key, values) in other) {
        val list = get(key) ?: ArrayList()
        list.addAll(values)
        if (list.isNotEmpty())
            this[key] = list
    }
}


fun KClass<*>.getMemberAnnotations(): MutableMap<String, MutableList<Annotation>> {
    val map = HashMap<String, MutableList<Annotation>>()
    if (annotations.isNotEmpty()) {
        map[""] = annotations.toMutableList()
    }
    constructors.forEach { constructor ->
        constructor.parameters.forEach { param ->
            val name = param.name!!
            val list = if (!map.containsKey(name)) {
                val listLocal = ArrayList<Annotation>()
                map[name] = listLocal
                listLocal
            } else {
                map[name]!!
            }
            list += param.annotations
            if (map[name]?.isEmpty() == true) {
                map.remove(name)
            }
        }
    }
    val members = members
    for (memberUnchecked in members) {
        val member = memberUnchecked as? KProperty<*> ?: continue
        val name = member.name
        val annotations = member.annotations
        if (annotations.isEmpty()) {
            continue
        }
        val list = if (!map.containsKey(name)) {
            val listLocal = ArrayList<Annotation>()
            map[name] = listLocal
            listLocal
        } else {
            map[name]!!
        }
        list += annotations
    }
    return map
}

fun KClass<*>.printAnnotations(prefix: String = "") {
    println("$this $prefix\nclass annotations ${this.annotations}")
    constructors.forEach {
        if (it.parameters[0].annotations.isNotEmpty()) {
            println("constructor annotation ${it.parameters[0].annotations}")
        }
    }
    val memberAnnotations = HashMap<String, ArrayList<Annotation>>()
    members.forEach { member ->
        val list = ArrayList<Annotation>()
        memberAnnotations[member.name] = list
        member.annotations.forEach { annotation ->
            list.add(annotation)
        }
        if (list.isEmpty()) {
            memberAnnotations.remove(member.name)
        }
    }
    if (memberAnnotations.isEmpty()) {
        return
    }
    println("member annotations $memberAnnotations\n\n")
}

annotation class PrimaryKey

@SQLTable
interface Y {
    @PrimaryKey
    val x: Int
    val y: String
}

fun main() {
    println(createTable<Y>())
}

annotation class Meow
annotation class Meow2


val SQLITE_KEY_WORDS = setOf(
    "ABORT", "ACTION", "ADD", "AFTER", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "ATTACH",
    "AUTOINCREMENT", "BEFORE", "BEGIN", "BETWEEN", "BY", "CASCADE", "CASE", "CAST", "CHECK", "COLLATE", "COLUMN",
    "COMMIT", "CONFLICT", "CONSTRAINT", "CREATE", "CROSS", "CURRENT", "CURRENT_DATE", "CURRENT_TIME",
    "CURRENT_TIMESTAMP", "DATABASE", "DEFAULT", "DEFERRABLE", "DEFERRED", "DELETE", "DESC", "DETACH", "DISTINCT",
    "DO", "DROP", "EACH", "ELSE", "END", "ESCAPE", "EXCEPT", "EXCLUDE", "EXCLUSIVE", "EXISTS", "EXPLAIN", "FAIL",
    "FILTER", "FOLLOWING", "FOR", "FOREIGN", "FROM", "FULL", "GLOB", "GROUP", "GROUPS", "HAVING", "IF", "IGNORE",
    "IMMEDIATE", "IN", "INDEX", "INDEXED", "INITIALLY", "INNER", "INSERT", "INSTEAD", "INTERSECT", "INTO", "IS",
    "ISNULL", "JOIN", "KEY", "LEFT", "LIKE", "LIMIT", "MATCH", "NATURAL", "NO", "NOT", "NOTHING", "NOTNULL", "NULL",
    "OF", "OFFSET", "ON", "OR", "ORDER", "OTHERS", "OUTER", "OVER", "PARTITION", "PLAN", "PRAGMA", "PRECEDING",
    "PRIMARY", "QUERY", "RAISE", "RANGE", "RECURSIVE", "REFERENCES", "REGEXP", "REINDEX", "RELEASE", "RENAME",
    "REPLACE", "RESTRICT", "RIGHT", "ROLLBACK", "ROW", "ROWS", "SAVEPOINT", "SELECT", "SET", "TABLE", "TEMP",
    "TEMPORARY", "THEN", "TIES", "TO", "TRANSACTION", "TRIGGER", "UNBOUNDED", "UNION", "UNIQUE", "UPDATE", "USING",
    "VACUUM", "VALUES", "VIEW", "VIRTUAL", "WHEN", "WHERE", "WINDOW", "WITH", "WITHOUT"
)