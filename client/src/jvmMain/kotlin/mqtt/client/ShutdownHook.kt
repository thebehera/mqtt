package mqtt.client

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import mqtt.client.platform.PlatformSocketConnection
import java.util.concurrent.atomic.AtomicBoolean

class ShutdownHook : Thread("MQTT Global Connection Shutdown Hook, clean disconnecting clients") {
    private val connections = HashSet<PlatformSocketConnection>()
    val isShuttingDown = AtomicBoolean(false)

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
//        println("Attempting to shutdown ${connections.size} connections")
        val localConnections = HashSet(connections)
        connections.clear()
        val jobs = mutableListOf<Deferred<Boolean>>()
        localConnections.forEach {
            jobs += it.closeAsync()
        }
        runBlocking {
            jobs.forEach { it.await() }
        }
//        println("Successfully disconnected ${jobs.size} connections due to shutdown signal")
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
