@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.sql

import kotlin.reflect.KClass
import kotlin.reflect.KProperty


inline fun <reified T> List<Annotation>.findInstanceOf(): T? {
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

fun String.escapeNameIfNeeded() =
    if (contains(".")
        || contains("(")
        || SQLITE_KEY_WORDS.contains(toUpperCase())
    ) {
        "`$this`"
    } else {
        this
    }

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
