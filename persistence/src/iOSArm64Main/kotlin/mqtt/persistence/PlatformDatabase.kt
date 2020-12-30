package mqtt.persistence

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import mqtt.persistence.db.Database
import mqtt.persistence.db.MqttQueries

actual fun getPlatformDatabase(name: String, contextProvider: ContextProvider): PlatformDatabase {
//    val driver = createDriver(name, contextProvider)
//    Database.Schema.create(driver)
//    val database = Database(driver)
//    database.mqttQueries.ignoreHost("")
    throw UnsupportedOperationException()
}

actual fun createDriver(name: String, contextProvider: ContextProvider): SqlDriver =
    NativeSqliteDriver(Database.Schema, name)
