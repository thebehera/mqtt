package mqtt.android_app.room

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey

@Entity
data class Test1(
    @PrimaryKey
    val id: Int,
    val data: String
)

@Dao
interface Test1Dao {
    @Insert
    fun insert(vararg objs: Test1)
}
