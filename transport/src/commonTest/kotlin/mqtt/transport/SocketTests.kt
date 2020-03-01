package mqtt.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import mqtt.transport.nio.socket.readStats
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

const val clientCount = 10L

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
@InternalCoroutinesApi
class SocketTests {
    private val connectTimeout = 30.seconds
    val validateCloseWait = true

    @Test
    fun nio2ConnectDisconnect() = block {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { asyncClientSocket() }
        connectDisconnect(serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nio2ConnectDisconnectStress() = block {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { asyncClientSocket() }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nio2ConnectDisconnectStressOpenConnections() = block {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { asyncClientSocket() }
        val serverLaunched = launchServer(this, serverSocket)
        stressTestOpenConnections(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nioNonBlockingConnectDisconnect() = block {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(false) }
        connectDisconnect(serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nioNonBlockingConnectDisconnectStress() = block {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(false) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nioNonBlockingConnectDisconnectStressOpenConnections() = block {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(false) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTestOpenConnections(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nioBlockingConnectDisconnect() = block {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(true) }
        connectDisconnect(serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nioBlockingConnectDisconnectStress() = block {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(true) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    @Test
    fun nioBlockingConnectDisconnectStressOpenConnections() = block {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(true) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTestOpenConnections(serverLaunched, serverSocket, clientSocket, validateCloseWait)
    }

    private suspend fun launchServer(
        scope: CoroutineScope,
        getServerSocket: () -> ServerSocket
    ): ServerLaunched {
        val serverSocket = getServerSocket()
        val firstReceiveLock = Mutex(true)
        val mutex = Mutex(true)
        var serverClientSocket: ClientSocket? = null
        serverSocket.bind()
        val server = Server(serverSocket)
        val launched = ServerLaunched(firstReceiveLock, server, serverSocket, serverClientSocket, mutex)
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
        val server: Server,
        val serverSocket: ServerSocket,
        var serverClientSocket: ClientSocket?,
        val mutex: Mutex,
        var count: Int = 0
    )


    fun connectDisconnect(
        getServerSocket: () -> ServerSocket, getClientSocket: () -> ClientToServerSocket,
        validateCloseWait: Boolean
    ) = block {
        val serverSocket = getServerSocket()
        assertFalse(serverSocket.isOpen())
        serverSocket.bind()
        assertTrue(serverSocket.isOpen())
        val port = serverSocket.port()!!
        val clientToServerSocket = getClientSocket()
        assertFalse(clientToServerSocket.isOpen())
        val mutex = Mutex(true)
        var clientCount = 0
        launch {
            clientToServerSocket.open(connectTimeout, port)
            clientCount++
            assertTrue(clientToServerSocket.isOpen())
            clientToServerSocket.close()
            assertFalse(clientToServerSocket.isOpen())
            mutex.unlock()
        }
        val serverToClientSocket = serverSocket.accept()
        assertTrue(serverToClientSocket.isOpen())
        serverToClientSocket.close()
        assertFalse(serverToClientSocket.isOpen())
        assertTrue(serverSocket.isOpen())
        serverSocket.close()
        assertFalse(serverSocket.isOpen())
        withTimeout(connectTimeout.toLongMilliseconds()) {
            mutex.lock()
        }
        assertEquals(1, clientCount, "Didn't execute client to server socket code")
        if (validateCloseWait) {
            val stats = readStats(port, "CLOSE_WAIT")
            if (stats.isNotEmpty()) {
                println("stats (${stats.count()}): $stats")
            }
            assertEquals(0, stats.count(), "Socket still in CLOSE_WAIT state found!")
        }
    }

    @InternalCoroutinesApi
    private suspend fun stressTest(
        serverL: ServerLaunched?,
        getServerSocket: () -> ServerSocket, getClientSocket: () -> ClientToServerSocket,
        validateCloseWait: Boolean = false
    ) =
        block {
            val serverLaunched = serverL ?: launchServer(this, getServerSocket)
            repeat(clientCount.toInt()) {
                try {
                    val client = getClientSocket()
                    serverLaunched.serverClientSocket = client
                    client.open(connectTimeout, serverLaunched.serverSocket.port()!!)
                    serverLaunched.firstMessageReceivedLock.lock()
                    assertTrue(client.isOpen())
                    val clientPort = client.localPort()!!
                    assertNotNull(serverLaunched.server.connections[clientPort])
                    client.close()
                    serverLaunched.server.closeClient(clientPort)
                    assertNull(serverLaunched.server.connections[clientPort])
                } catch (e: Throwable) {
                    println("Failed to validate client #$it")
                    throw e
                } finally {
                    if (validateCloseWait) {
                        val stats = serverLaunched.server.getStats()
                        if (stats.isNotEmpty()) {
                            println("stats (${stats.count()}): $stats")
                        }
                        assertEquals(0, stats.count(), "Socket still in CLOSE_WAIT state found!")
                    }
                }
            }
            serverLaunched.mutex.lock()
            serverLaunched.serverSocket.close()
            assertEquals(clientCount, serverLaunched.count.toLong())
        }


    private suspend fun stressTestOpenConnections(
        serverL: ServerLaunched?,
        getServerSocket: () -> ServerSocket, getClientSocket: () -> ClientToServerSocket,
        validateCloseWait: Boolean = false
    ) =
        block {
            val serverLaunched = serverL ?: launchServer(this, getServerSocket)
            val clients = ArrayList<ClientSocket>(clientCount.toInt())
            repeat(clientCount.toInt()) {
                val client = getClientSocket()
                serverLaunched.serverClientSocket = client
                client.open(connectTimeout, serverLaunched.serverSocket.port()!!)
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
                if (validateCloseWait) {
                    val stats = serverLaunched.server.getStats()
                    if (stats.isNotEmpty()) {
                        println("stats (${stats.count()}): $stats")
                    }
                    assertEquals(0, stats.count(), "Socket still in CLOSE_WAIT state found!")
                }
            }
            assertEquals(0, serverLaunched.server.connections.count().toLong())
            serverLaunched.mutex.lock()
            serverLaunched.serverSocket.close()
            assertEquals(clientCount, serverLaunched.count.toLong())
        }

}
