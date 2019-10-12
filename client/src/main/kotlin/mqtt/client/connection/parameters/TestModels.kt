package mqtt.client.connection.parameters

//
//class LibaryCode {
//    abstract class MqttRoomDatabase : RoomDatabase() {
//        abstract fun remoteHostDao(): RemoteHostDao
//    }
//}
//
//class ApplicationCode {
//
//    @Entity
//    data class TestModel1(@PrimaryKey val x: Int, val s: String)
//
//    @Dao
//    interface TestAppDao1 {
//        @Insert
//        fun insertTestModel1(model1: ApplicationCode.TestModel1)
//    }
//
//
//    @MqttDatabase
//    @Database(entities = [TestModel1::class, RemoteHost::class], version = 1)
//    abstract class TestAppMqttDb : LibaryCode.MqttRoomDatabase() {
//        abstract fun testApp1Dao(): TestAppDao1
//
//        fun getRoomDB(context: Context) {
//            Room.databaseBuilder<TestAppMqttDb>(context,
//                Class.forName("yolo") as Class<TestAppMqttDb>, "yolo")
//        }
//    }
//}
//


