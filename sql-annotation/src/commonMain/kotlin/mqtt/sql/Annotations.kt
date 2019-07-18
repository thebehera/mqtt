package mqtt.sql

import mqtt.sql.ForeignKeyActions.NoAction
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.reflect.KClass

@MustBeDocumented
annotation class SQLTable(
    val database: String = "app.db",
    val tableName: String = "",
    val primaryKeys: Array<String> = [],
    val subclasses: Array<KClass<out Any>> = []
)

annotation class Unique

annotation class PrimaryKey


annotation class Check(val expression: String)

@Target(FIELD)
annotation class SQLColumn(val name: String = "", val primaryKey: Boolean = false)

enum class ForeignKeyActions(val action: String) {
    NoAction("NO ACTION"),
    Restrict("RESTRICT"),
    SetNull("SET NULL"),
    SetDefault("SET DEFAULT"),
    Cascade("CASCADE")

}

annotation class ForeignKey(
    val table: KClass<*>,
    val column: String,
    val onDelete: ForeignKeyActions = NoAction,
    val onUpdate: ForeignKeyActions = NoAction
)
