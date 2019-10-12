package mqtt.androidx.room

interface IRemoteHostDao<T> {
    suspend fun addOrUpdate(host: T)

    suspend fun getAllConnections(): List<T>

    suspend fun remove(host: T)
}
