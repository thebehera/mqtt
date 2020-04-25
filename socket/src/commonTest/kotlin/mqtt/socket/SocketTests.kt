@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.allocateNewBuffer
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

const val clientCount = 100L

@OptIn(ExperimentalTime::class)
class SocketTests {
    private val connectTimeout = 30.seconds
    private val writeTimeout = 100.milliseconds
    private val readTimeout = writeTimeout
    val validateCloseWait = true
    val validateCloseWaitAgressive = false

    @Test
    fun client() = blockIgnoreUnsupported {
        console.log("start")
        val s = asyncClientSocket()
        console.log("open")
        s.open(1.seconds, 8080u)

        console.log("opened")
        val buffer = allocateNewBuffer(20u, limits)

        val buffer2 = allocateNewBuffer(20u, limits)
        val mutex1 = Mutex(true)
        launch {
            buffer.writeUtf8String("hi\r\n")
            s.write(buffer, 1.seconds)
            mutex1.unlock()
        }
        val m2 = Mutex(true)
        launch {
            s.read(buffer2, 1.seconds)
            m2.unlock()
        }
        console.log(buffer2.toString())
        mutex1.lock()
        m2.lock()
        console.log("unlock")
        s.close()
    }

    @Test
    fun nio2ConnectClientReadWriteDisconnect() = blockIgnoreUnsupported {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { asyncClientSocket() }
        connectClientReadWriteDisconnect(this, serverSocket, clientSocket)
    }

    @Test
    fun nio2ConnectDisconnectStress() = blockIgnoreUnsupported {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { asyncClientSocket() }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(this, serverLaunched, serverSocket, clientSocket)
    }

    @Test
    fun nio2ConnectDisconnectStressOpenConnections() = blockIgnoreUnsupported {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { asyncClientSocket() }
        val serverLaunched = launchServer(this, serverSocket)
        stressTestOpenConnections(this, serverLaunched, serverSocket, clientSocket)
    }

    @Test
    fun nioNonBlockingConnectClientReadWriteDisconnect() = blockIgnoreUnsupported {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(false) }
        connectClientReadWriteDisconnect(this, serverSocket, clientSocket)
    }

    @Test
    fun nioNonBlockingConnectDisconnectStress() = blockIgnoreUnsupported {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(false) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(this, serverLaunched, serverSocket, clientSocket)
    }

    @Test
    fun nioNonBlockingConnectDisconnectStressOpenConnections() = blockIgnoreUnsupported {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(false) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTestOpenConnections(this, serverLaunched, serverSocket, clientSocket)
    }

    @Test
    fun nioBlockingConnectClientReadWriteDisconnect() = blockIgnoreUnsupported {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(true) }
        connectClientReadWriteDisconnect(this, serverSocket, clientSocket)
    }

    @Test
    fun nioBlockingConnectDisconnectStress() = blockIgnoreUnsupported {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(true) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(this, serverLaunched, serverSocket, clientSocket)
    }

    @Test
    fun nioBlockingConnectDisconnectStressOpenConnections() = blockIgnoreUnsupported {
        val serverSocket = { asyncServerSocket() }
        val clientSocket = { clientSocket(true) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTestOpenConnections(this, serverLaunched, serverSocket, clientSocket)
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

    val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    private suspend fun connectClientReadWriteDisconnect(
        scope: CoroutineScope,
        getServerSocket: () -> ServerSocket, getClientSocket: () -> ClientToServerSocket
    ) {
        val expectedClientToServer = 4.toUShort()
        val expectedServerToClient = UInt.MAX_VALUE
        val clientWriteBuffer = allocateNewBuffer(10.toUInt(), limits)
        clientWriteBuffer.write(expectedClientToServer)
        val clientReadBuffer = allocateNewBuffer(10.toUInt(), limits)
        val serverSocket = getServerSocket()
        assertFalse(serverSocket.isOpen())
        serverSocket.bind()
        assertTrue(serverSocket.isOpen())
        val port = serverSocket.port()!!
        val clientToServerSocket = getClientSocket()
        assertFalse(clientToServerSocket.isOpen())
        var clientCount = 0
        val clientDoneMutex = Mutex(true)
        scope.launch {
            clientToServerSocket.open(connectTimeout, port)
            assertEquals(1, ++clientCount)
            assertTrue(clientToServerSocket.isOpen())
            assertEquals(2, clientToServerSocket.write(clientWriteBuffer, writeTimeout), "client write")
            assertEquals(4, clientToServerSocket.read(clientReadBuffer, readTimeout), "client read")
            assertEquals(expectedServerToClient, clientReadBuffer.readUnsignedInt(), "client wrong value")
            clientDoneMutex.unlock()
        }
        val serverToClientSocket = serverSocket.accept()
        assertTrue(serverToClientSocket.isOpen())
        val serverReadBuffer = allocateNewBuffer(10.toUInt(), limits)
        assertEquals(2, serverToClientSocket.read(serverReadBuffer, readTimeout), "server read")
        assertEquals(expectedClientToServer, serverReadBuffer.readUnsignedShort())
        val serverWriteBuffer = allocateNewBuffer(10.toUInt(), limits)
        serverWriteBuffer.write(expectedServerToClient)
        assertEquals(4, serverToClientSocket.write(serverWriteBuffer, writeTimeout), "server write")
        clientDoneMutex.lock()
        assertTrue(serverToClientSocket.isOpen())
        serverToClientSocket.close()
        assertFalse(serverToClientSocket.isOpen())
        assertTrue(clientToServerSocket.isOpen())
        clientToServerSocket.close()
        assertFalse(clientToServerSocket.isOpen())
        assertTrue(serverSocket.isOpen())
        serverSocket.close()
        assertFalse(serverSocket.isOpen())
        assertEquals(1, clientCount, "Didn't execute client to server socket code")
        if (validateCloseWait || validateCloseWaitAgressive) {
            val stats = readStats(port, "CLOSE_WAIT")
            if (stats.isNotEmpty()) {
                println("stats (${stats.count()}): $stats")
            }
            assertEquals(0, stats.count(), "Socket still in CLOSE_WAIT state found!")
        }
    }

    private suspend fun stressTest(
        scope: CoroutineScope,
        serverL: ServerLaunched?,
        getServerSocket: () -> ServerSocket, getClientSocket: () -> ClientToServerSocket
    ) {
        val serverLaunched = serverL ?: launchServer(scope, getServerSocket)
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
                if (validateCloseWaitAgressive) {
                    checkPort(serverLaunched.server)
                }
            }
        }
        serverLaunched.mutex.lock()
        if (validateCloseWait) {
            checkPort(serverLaunched.server)
        }
        serverLaunched.serverSocket.close()
        assertEquals(clientCount, serverLaunched.count.toLong())
    }


    private suspend fun stressTestOpenConnections(
        scope: CoroutineScope,
        serverL: ServerLaunched?,
        getServerSocket: () -> ServerSocket, getClientSocket: () -> ClientToServerSocket
    ) {
        val serverLaunched = serverL ?: launchServer(scope, getServerSocket)
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
            if (validateCloseWaitAgressive) {
                checkPort(serverLaunched.server)
            }
        }
        assertEquals(0, serverLaunched.server.connections.count().toLong())
        serverLaunched.mutex.lock()
        if (validateCloseWait) {
            checkPort(serverLaunched.server)
        }
        serverLaunched.serverSocket.close()
        assertEquals(clientCount, serverLaunched.count.toLong())
    }

    private fun checkPort(server: Server) {
        val stats = readStats(server.serverSocket.port()!!, "CLOSE_WAIT")
        if (stats.isNotEmpty()) {
            println("stats (${stats.count()}): $stats")
        }
        assertEquals(0, stats.count(), "Socket still in CLOSE_WAIT state found!")
    }

}
