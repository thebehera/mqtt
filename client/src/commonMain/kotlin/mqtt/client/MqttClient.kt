package mqtt.client


interface Persistence {
    fun persist()

}

interface Engine {
    //    val connection: SocketConnection
    val messageQueue: Persistence
}

interface Client {
    val engine: Engine

}
