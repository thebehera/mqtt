package mqtt.client

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking

class ShutdownHook : Thread("MQTT Global Connection Shutdown Hook, clean disconnecting clients") {
    private val connections = HashSet<PlatformSocketConnection>()

    fun addConnection(socketConnection: PlatformSocketConnection) {
        if (connections.size == 0) {
            Runtime.getRuntime().addShutdownHook(this)
        }
        connections += socketConnection
    }

    override fun run() {
        println("Shut down received, closing ${connections.size} connections")
        val localConnections = HashSet(connections)
        connections.clear()
        val jobs = mutableListOf<Job>()
        localConnections.forEach { jobs += it.closeAsync() }
        runBlocking {
            jobs.forEach { it.join() }
        }
        println("Successfully shut down connections")
        if (!connections.isEmpty()) {
            run()
        }
    }

    fun removeConnections(socketConnection: PlatformSocketConnection) {
        connections -= socketConnection
        if (connections.size == 0) {
            try {
                Runtime.getRuntime().removeShutdownHook(this)
            } catch (e: IllegalStateException) {
                // ignore because we are shutting down
            }
        }
    }

    companion object {
        internal val shutdownThread by lazy { ShutdownHook() }
    }
}
