package mqtt.client

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import mqtt.client.platform.PlatformSocketConnection

class ShutdownHook : Thread("MQTT Global Connection Shutdown Hook, clean disconnecting clients") {
    private val connections = HashSet<PlatformSocketConnection>()
    init {
        Runtime.getRuntime().addShutdownHook(this)
    }
    fun addConnection(socketConnection: PlatformSocketConnection) {
        connections += socketConnection
    }

    override fun run() {
        if (connections.isEmpty()) {
            return
        }
        val localConnections = HashSet(connections)
        connections.clear()
        val jobs = mutableListOf<Deferred<Boolean>>()
        localConnections.forEach {
            jobs += it.closeAsync()
        }
        runBlocking {
            jobs.forEach { it.await() }
        }
        if (jobs.isNotEmpty()) {
            run()
        }
    }

    fun removeConnections(socketConnection: PlatformSocketConnection) {
        connections -= socketConnection
    }

    companion object {
        internal val shutdownThread by lazy { ShutdownHook() }
    }
}
