package mqtt.sql

import kotlin.annotation.AnnotationTarget.FIELD

@MustBeDocumented
annotation class SQLTable(
    val database: String = "app.db",
    val tableName: String = "",
    val primaryKeys: Array<String> = []
)

@Target(FIELD)
annotation class UniqueConstraint

annotation class PrimaryKey

@Target(FIELD)
annotation class CheckConstraint

@Target(FIELD)
annotation class SQLColumn(val name: String = "", val primaryKey: Boolean = false)
