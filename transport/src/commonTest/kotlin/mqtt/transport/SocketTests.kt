package mqtt.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

const val clientCount = 100L

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class SocketTests {

    val validateCloseWait = true
    val pool = BufferPool(limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000_000u
    })


    @Test
    fun nio2ConnectDisconnectStress() = block {
        val serverSocket = { asyncServerSocket(this, 10.milliseconds, 10.milliseconds, pool) }
        val clientSocket = { asyncClientSocket(this, 10.milliseconds, 10.milliseconds, pool) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nio2ConnectDisconnectStressOpenConnections() = block {
        val serverSocket = { asyncServerSocket(this, 10.milliseconds, 10.milliseconds, pool) }
        val clientSocket = { asyncClientSocket(this, 10.milliseconds, 10.milliseconds, pool) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTestOpenConnections(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nioNonBlockingConnectDisconnectStress() = block {
        val serverSocket = { asyncServerSocket(this, 10.milliseconds, 10.milliseconds, pool) }
        val clientSocket = { clientSocket(this, false, 10.milliseconds, 10.milliseconds, pool) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nioNonBlockingConnectDisconnectStressOpenConnections() = block {
        val serverSocket = { asyncServerSocket(this, 10.milliseconds, 10.milliseconds, pool) }
        val clientSocket = { clientSocket(this, false, 10.milliseconds, 10.milliseconds, pool) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTestOpenConnections(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nioBlockingConnectDisconnectStress() = block {
        val serverSocket = { asyncServerSocket(this, 10.milliseconds, 10.milliseconds, pool) }
        val clientSocket = { clientSocket(this, true, 10.milliseconds, 10.milliseconds, pool) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nioBlockingConnectDisconnectStressOpenConnections() = block {
        val serverSocket = { asyncServerSocket(this, 10.milliseconds, 10.milliseconds, pool) }
        val clientSocket = { clientSocket(this, true, 10.milliseconds, 10.milliseconds, pool) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTestOpenConnections(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    private suspend fun launchServer(
        scope: CoroutineScope,
        getServerSocket: () -> ServerToClientSocket
    ): ServerLaunched {
        val server = getServerSocket()
        val firstReceiveLock = Mutex(true)
        val mutex = Mutex(true)
        var serverClientSocket: ClientSocket? = null
        val launched = ServerLaunched(firstReceiveLock, server, serverClientSocket, mutex)
        server.bind()
        scope.launch {
            server.listen().collect {
                serverClientSocket = it
                launched.serverClientSocket = it
                firstReceiveLock.unlock()
                if (++launched.count >= clientCount) {
                    mutex.unlock()
                    return@collect
                }
            }
        }
        return launched
    }

    data class ServerLaunched(
        val firstMessageReceivedLock: Mutex,
        val server: ServerToClientSocket,
        var serverClientSocket: ClientSocket?,
        val mutex: Mutex,
        var count: Int = 0
    )

    private suspend fun stressTest(
        serverL: ServerLaunched?,
        getServerSocket: () -> ServerToClientSocket, getClientSocket: () -> ClientToServerSocket,
        validateCloseWait: Boolean = false
    ) =
        block {
            val serverLaunched = serverL ?: launchServer(this, getServerSocket)
            repeat(clientCount.toInt()) {
                val client = getClientSocket()
                serverLaunched.serverClientSocket = client
                client.tag = it.toString()
                client.open(port = serverLaunched.server.port()!!)
                serverLaunched.firstMessageReceivedLock.lock()
                assertTrue(client.isOpen())
                val clientPort = client.localPort()!!
                assertNotNull(serverLaunched.server.connections[clientPort])
                client.close()
                serverLaunched.server.closeClient(clientPort)
                assertNull(serverLaunched.server.connections[clientPort])
            }
            if (validateCloseWait) {
                val stats = serverLaunched.server.getStats()
                if (stats.isNotEmpty()) {
                    println("stats (${stats.count()}): $stats")
                }
                assertEquals(0, stats.count(), "Socket still in CLOSE_WAIT state found!")
            }
            serverLaunched.mutex.lock()
            serverLaunched.server.close()
            assertEquals(clientCount, serverLaunched.count.toLong())
        }


    private suspend fun stressTestOpenConnections(
        serverL: ServerLaunched?,
        getServerSocket: () -> ServerToClientSocket, getClientSocket: () -> ClientToServerSocket,
        validateCloseWait: Boolean = false
    ) =
        block {
            val serverLaunched = serverL ?: launchServer(this, getServerSocket)
            val clients = ArrayList<ClientSocket>(clientCount.toInt())
            repeat(clientCount.toInt()) {
                val client = getClientSocket()
                serverLaunched.serverClientSocket = client
                client.tag = it.toString()
                client.open(port = serverLaunched.server.port()!!)
                serverLaunched.firstMessageReceivedLock.lock()
                clients += client
            }
            assertEquals(clientCount, clients.count().toLong())
            assertEquals(clientCount, serverLaunched.server.connections.count().toLong())
            clients.forEach {
                val port = it.localPort()!!
                assertTrue(it.isOpen())
                it.close()
                serverLaunched.server.closeClient(port)
            }
            if (validateCloseWait) {
                val stats = serverLaunched.server.getStats()
                if (stats.isNotEmpty()) {
                    println("stats (${stats.count()}): $stats")
                }
                assertEquals(0, stats.count(), "Socket still in CLOSE_WAIT state found!")
            }
            assertEquals(0, serverLaunched.server.connections.count().toLong())
            serverLaunched.mutex.lock()
            serverLaunched.server.close()
            assertEquals(clientCount, serverLaunched.count.toLong())
        }

}
