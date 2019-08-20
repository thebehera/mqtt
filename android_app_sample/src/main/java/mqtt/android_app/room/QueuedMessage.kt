@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.android_app.room

import android.content.Context
import androidx.room.*

@Entity
data class QueuedMessage(
    @PrimaryKey val queuedObjectId: Long,
    val tableName: String,
    val rowId: Long,
    // Null until first attempt to deque
    val messageId: UShort?,
    val priority: Double = 0.0,
    val deliveryState: Int
)


// Different states:
// Get a message valid messageId
@Dao
interface QueuedDao {
    @Insert
    fun queue(vararg objs: QueuedMessage)

    @Delete
    fun deleteFromQueue(obj: QueuedMessage)

    @Query("SELECT MAX(messageId) FROM QueuedMessage")
    fun largestCurrentMessageId(): UShort

    @Query("INSERT INTO QueuedMessage DEFAULT VALUES;")
    fun createQueuedObject(): Long
}


@Database(entities = [Test1::class, QueuedMessage::class], version = 1)
abstract class QueuedDb : RoomDatabase() {
    abstract fun test1Dao(): Test1Dao
    abstract fun queuedDao(): QueuedDao


    fun addConstraints() {
        runInTransaction {

        }
    }

    fun queue(obj: QueuedMessage) {

        val nextMessageId = queuedDao().createQueuedObject()

    }
}


lateinit var appContext: Context

fun initQueuedDb(context: Context): QueuedDb {
    if (::appContext.isInitialized) {
        return queuedDb
    }
    appContext = context.applicationContext
    return queuedDb
}

val queuedDb by lazy {
    Room.databaseBuilder(appContext, QueuedDb::class.java, "testdb").build()
}
