package mqtt.client

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

class ShutdownHook : Thread("MQTT Global Connection Shutdown Hook, clean disconnecting clients") {
    private val connections = HashSet<PlatformSocketConnection>()
    val isShuttingDown = AtomicBoolean(false)

    fun addConnection(socketConnection: PlatformSocketConnection) {
        if (connections.size == 0) {
            Runtime.getRuntime().addShutdownHook(this)
        }
        connections += socketConnection
    }

    override fun run() {
        isShuttingDown.set(true)
        println("Shut down received, closing ${connections.size} connections")
        val localConnections = HashSet(connections)
        connections.clear()
        val jobs = mutableListOf<Deferred<Boolean>>()
        localConnections.forEach {
            jobs += it.closeAsync()
        }
        runBlocking {
            jobs.forEach { it.await() }
        }
        println("Successfully shut down ${jobs.size} connections")
        if (connections.isNotEmpty()) {
            run()
        }
        isShuttingDown.set(false)
    }

    fun removeConnections(socketConnection: PlatformSocketConnection) {
        connections -= socketConnection
        if (connections.size == 0 && !isShuttingDown.get()) {
            try {
                Runtime.getRuntime().removeShutdownHook(this)
            } catch (e: IllegalStateException) {
                println("illegal state")
                // ignore because we are shutting down
            }
        }
    }

    companion object {
        internal val shutdownThread by lazy { ShutdownHook() }
    }
}
