package mqtt.client.connection.parameters

import androidx.room.*


class LibaryCode {
    annotation class MqttDatabase(val roomDbAnnotation: Database)

    abstract class MqttRoomDatabase : RoomDatabase() {
        abstract fun remoteHostDao(): RemoteHostDao
    }
}

class ApplicationCode {

    @Entity
    data class TestModel1(@PrimaryKey val x: Int, val s: String)

    @Dao
    interface TestAppDao1 {
        @Insert
        fun insertTestModel1(model1: ApplicationCode.TestModel1)
    }


    @LibaryCode.MqttDatabase(roomDbAnnotation = Database(entities = [TestModel1::class], version = 1))
    abstract class TestAppMqttDb : LibaryCode.MqttRoomDatabase() {
        abstract fun testApp1Dao(): TestAppDao1
    }
}



