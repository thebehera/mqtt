package mqtt.client.connection.parameters

import androidx.room.*

@Dao
interface RemoteHostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdate(host: PersistableRemoteHostV4)

    @Query("SELECT * FROM PersistableRemoteHostV4")
    suspend fun getAllConnections(): List<PersistableRemoteHostV4>

    @Delete
    suspend fun remove(host: PersistableRemoteHostV4)
}