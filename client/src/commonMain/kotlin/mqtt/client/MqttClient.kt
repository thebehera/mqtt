package mqtt.client


interface Persistence {
    fun persist()
}



interface Engine {
    val connection: AbstractSocketConnection
    val messageQueue: Persistence
}

interface Client {
    val engine: Engine

    suspend fun publish(topic: String, payload:ByteArray)

    suspend fun <T> subscribe(topic: String, payload: T)

}
