package mqtt.sql

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD

@Target(CLASS)
@Retention(SOURCE)
@MustBeDocumented
annotation class SQLTable(
    val database: String = "app.db",
    val tableName: String = "",
    val primaryKeys: Array<String>
)

@Target(FIELD)
annotation class UniqueConstraint

@Target(FIELD)
annotation class PrimaryKey

@Target(FIELD)
annotation class CheckConstraint

@Target(FIELD)
annotation class SQLColumn(val name: String = "", val primaryKey: Boolean = false)
