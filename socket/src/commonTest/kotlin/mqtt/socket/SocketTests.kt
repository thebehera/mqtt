@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
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
    private val clientCount1 = 100L
    private val writeTimeout = 100.milliseconds
    private val readTimeout = writeTimeout
    private val validateCloseWait = true
    private val validateCloseWaitAgressive = false

    @ExperimentalUnsignedTypes
    @Test
    fun nio2ConnectClientReadWriteDisconnect() = block {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.USHORT)) }
        val clientSocket = { asyncClientSocket() }
        connectClientReadWriteDisconnect(this, serverSocket, clientSocket)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nio2ConnectDisconnectStress() = block {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT)) }
        val clientSocket = { asyncClientSocket() }
        stressDisconnectTest(serverSocket, clientSocket)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nio2ConnectDisconnectStressOpenConnections() = block {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT)) }
        val clientSocket = { asyncClientSocket() }
        stressTestOpenConnections(serverSocket, clientSocket)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioNonBlockingConnectClientReadWriteDisconnect() = block {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.USHORT)) }
        val clientSocket = { clientSocket(false) }
        connectClientReadWriteDisconnect(this, serverSocket, clientSocket)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioNonBlockingConnectDisconnectStress() = block {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT))}
        val clientSocket = { clientSocket(false) }
        stressDisconnectTest(serverSocket, clientSocket)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioNonBlockingConnectDisconnectStressOpenConnections() = block {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT))  }
        val clientSocket = { clientSocket(false) }
        stressTestOpenConnections(serverSocket, clientSocket)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioBlockingConnectClientReadWriteDisconnect() = block {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.USHORT)) }
        val clientSocket = { clientSocket(true) }
        connectClientReadWriteDisconnect(this, serverSocket, clientSocket)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioBlockingConnectDisconnectStress() = block {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT)) }
        val clientSocket = { clientSocket(true) }
        stressDisconnectTest(serverSocket, clientSocket)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioBlockingConnectDisconnectStressOpenConnections() = block {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT)) }
        val clientSocket = { clientSocket(true) }
        stressTestOpenConnections(serverSocket, clientSocket)
    }

    @ExperimentalUnsignedTypes
    private val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    @ExperimentalUnsignedTypes
    private suspend fun connectClientReadWriteDisconnect(
        scope: CoroutineScope,
        getServerSocket: () -> TCPServer, getClientSocket: () -> ClientToServerSocket
    ) {
        val expectedClientToServer = 4.toUShort()
        val expectedServerToClient = UInt.MAX_VALUE
        val clientWriteBuffer = allocateNewBuffer(10.toUInt(), limits)
        clientWriteBuffer.write(expectedClientToServer)
        val clientReadBuffer = allocateNewBuffer(10.toUInt(), limits)
        val server = launchServer(getServerSocket())

        val port = server.getListenPort()
        val clientToServerSocket = getClientSocket()
        assertFalse (clientToServerSocket.isOpen())
        initiateClient(clientToServerSocket, port)

        val clientDoneMutex = Mutex(true)

        scope.launch {
            assertEquals(2, clientToServerSocket.write(clientWriteBuffer, writeTimeout), "client write")
            assertEquals(4, clientToServerSocket.read(clientReadBuffer, readTimeout), "client read")
            assertEquals(expectedServerToClient, clientReadBuffer.readUnsignedInt(), "client wrong value")
            clientDoneMutex.unlock()
        }

        clientDoneMutex.lock()
        assertTrue(server.isOpen(), "Server has closed")
        server.close()
        assertFalse(server.isOpen(), "Server is still open")
        assertTrue(clientToServerSocket.isOpen())
        clientToServerSocket.close()
        assertFalse(clientToServerSocket.isOpen())

        if (validateCloseWait || validateCloseWaitAgressive) {
            val stats = readStats(port, "CLOSE_WAIT")
            if (stats.isNotEmpty()) {
                println("stats (${stats.count()}): $stats")
            }
            assertEquals(0, stats.count(), "Socket still in CLOSE_WAIT state found!")
        }
    }

    @ExperimentalUnsignedTypes
    private suspend fun stressDisconnectTest(
        getServerSocket: () -> TCPServer, getClientSocket: () -> ClientToServerSocket
    ) {
        val clientDoneMutex = Mutex(locked = false)
        var port: UShort = 0u
        var counter = 0

        try {
            val server = launchServer(getServerSocket())

            port = server.getListenPort()

            clientDoneMutex.lock()
            repeat (clientCount1.toInt()) {
                val client = getClientSocket()
                assertFalse (client.isOpen())
                initiateClient(client, port)
                assertTrue(client.isOpen(), "Client connection is not open")
                client.close()
                assertFalse(client.isOpen(), "Client connetion is still open")
                counter++

                if (counter >= clientCount1)
                    clientDoneMutex.unlock()
            }

            clientDoneMutex.lock()
            assertTrue(server.isOpen(), "Server has closed")
            server.close()
            assertFalse(server.isOpen(), "Server is still open")

        } catch (e: Exception) {
            throw e
        } finally {
            if (port > 0u) {
                delay(200) // gave a delay to ensure surver has time to process the close() request
                checkPort(port)
            }

        }
    }

    @ExperimentalUnsignedTypes
    private suspend fun stressTestOpenConnections(getServerSocket: () -> TCPServer,
                                                  getClientSocket: () -> ClientToServerSocket) {
        val clients = ArrayList<ClientSocket>(clientCount.toInt())
        val clientDoneMutex = Mutex(locked = false)
        val clientCount = 50
        var counter = 0
        try {
            val server = launchServer(getServerSocket())
            val port = server.getListenPort()

            clientDoneMutex.lock()
            repeat(clientCount) {
                val client = getClientSocket()
                assertFalse (client.isOpen())
                initiateClient(client, port)
                assertTrue(client.isOpen(), "Client connection is not open")
                clients.add(client)
            }
            assertEquals(clientCount.toLong(), clients.size.toLong(), "correct amount of clients not initiated.")

            clients.forEach {
                it.close()
                assertFalse(it.isOpen(), "client is not closed.")
                counter++
                if (counter >= clientCount)
                    clientDoneMutex.unlock()
            }

            clientDoneMutex.lock()
            assertTrue(server.isOpen(), "Server has closed")
            server.close()
            assertFalse(server.isOpen(), "Server is still open")
        } catch (e: Exception) {
            println("stressTestOpenConnections.exception: ${e.message}, $e")
        }
    }

    @ExperimentalUnsignedTypes
    private fun checkPort(port: UShort) {
        val stats = readStats(port, "CLOSE_WAIT")
        if (stats.isNotEmpty()) {
            println("stats (${stats.count()}): $stats")
        }
        assertEquals(0, stats.count(), "Socket still in CLOSE_WAIT state found!")
    }

    @ExperimentalUnsignedTypes
    private suspend fun initiateClient(socket: ClientToServerSocket, port: UShort) {

        try {
            assertFalse(socket.isOpen(), "Client socket should not be open state")
            socket.open(100.seconds, port, "localhost")

            assertEquals(socket.remotePort(), port, "Remote port is not the as in connect request.")

            assertTrue(socket.isOpen(), "Connected to server, thus should be in open state")
        } catch (e: Exception) {
            println("socketTest.initiateClient.exception: $port")
            assertEquals("Failure....", e.message, "socketTest.exception")
        }
    }

    @ExperimentalUnsignedTypes
    private suspend fun launchServer(server: TCPServer) : TCPServer {
        server.startServer()
        assertNotEquals(server.getListenPort(), server.port, "Server listen port is diferent")
        GlobalScope.launch {
            server.getClientConnection()
            assertFalse(server.isOpen(), "Server socket is still open.")
        }
        assertTrue(server.isOpen(), "Server socket is not open.")
        return server
    }

}
