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

const val clientCount = 1000L

@OptIn(ExperimentalTime::class)
class SocketTests {
    private val clientCount1 = 1000L
    private val validateCloseWait = true
    private val validateCloseWaitAgressive = false

    @ExperimentalUnsignedTypes
    @Test
    fun nio2ConnectClientReadWriteDisconnect() = blockIgnoreUnsupported {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.USHORT)) }
        val clientProcess = { ClientProcessTest(ClientAction.USHORT) }
        connectClientReadWriteDisconnect(this, serverSocket, clientProcess)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nio2ConnectDisconnectStress() = blockIgnoreUnsupported {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT)) }
        val clientProcess = {ClientProcessTest(ClientAction.CONNECT_DISCONNECT)}
        stressDisconnectTest(serverSocket, clientProcess)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nio2ConnectDisconnectStressOpenConnections() = blockIgnoreUnsupported {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT)) }
        val clientProcess = {ClientProcessTest(ClientAction.CONNECT_DISCONNECT)}
        stressTestOpenConnections(serverSocket, clientProcess)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioNonBlockingConnectClientReadWriteDisconnect() = blockIgnoreUnsupported {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.USHORT)) }
        val clientProcess = { ClientProcessTest(ClientAction.USHORT, ConnectionType.NON_BLOCKING) }
        connectClientReadWriteDisconnect(this, serverSocket, clientProcess)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioNonBlockingConnectDisconnectStress() = blockIgnoreUnsupported {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT)) }
        val clientProcess = {ClientProcessTest(ClientAction.CONNECT_DISCONNECT, ConnectionType.NON_BLOCKING)}
        stressDisconnectTest(serverSocket, clientProcess)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioNonBlockingConnectDisconnectStressOpenConnections() = blockIgnoreUnsupported {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT)) }
        val clientProcess = {ClientProcessTest(ClientAction.CONNECT_DISCONNECT, ConnectionType.NON_BLOCKING)}
        stressTestOpenConnections(serverSocket, clientProcess)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioBlockingConnectClientReadWriteDisconnect() = blockIgnoreUnsupported {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.USHORT)) }
        val clientProcess = { ClientProcessTest(ClientAction.USHORT, ConnectionType.BLOCKING) }
        connectClientReadWriteDisconnect(this, serverSocket, clientProcess)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioBlockingConnectDisconnectStress() = blockIgnoreUnsupported {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT)) }
        //val clientSocket = { clientSocket(true) }
        val clientProcess = {ClientProcessTest(ClientAction.CONNECT_DISCONNECT, ConnectionType.BLOCKING)}
        stressDisconnectTest(serverSocket, clientProcess)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun nioBlockingConnectDisconnectStressOpenConnections() = blockIgnoreUnsupported {
        val serverSocket = { TCPServer("localhost", 0u, ServerProcessTest(ServerAction.CONNECT_DISCONNECT)) }
        val clientProcess = {ClientProcessTest(ClientAction.CONNECT_DISCONNECT, ConnectionType.BLOCKING)}
        stressTestOpenConnections(serverSocket, clientProcess)
    }

    @ExperimentalUnsignedTypes
    private val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    @ExperimentalUnsignedTypes
    private suspend fun connectClientReadWriteDisconnect(
        scope: CoroutineScope,
        getServerSocket: () -> TCPServer, getClientProcess: () -> ClientProcess //getClientSocket: () -> ClientToServerSocket
    ) {
        val expectedClientToServer = 4.toUShort()
        val clientWriteBuffer = allocateNewBuffer(10.toUInt(), limits)
        clientWriteBuffer.write(expectedClientToServer)
        val server = launchServer(getServerSocket())

        val port = server.getListenPort()

        val clientDoneMutex = Mutex(true)

        scope.launch {
            val client = getClientProcess()

            client.connect("localhost", port)
            assertFalse(client.isOpen())
            clientDoneMutex.unlock()
        }

        clientDoneMutex.lock()
        assertTrue(server.isOpen(), "Server has closed")
        server.close()
        assertFalse(server.isOpen(), "Server is still open")

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
        getServerSocket: () -> TCPServer, getClientProcess: () -> ClientProcess //getClientSocket: () -> ClientToServerSocket
    ) {
        val clientDoneMutex = Mutex(locked = false)
        var port: UShort = 0u
        var counter = 0

        try {
            val server = launchServer(getServerSocket())

            port = server.getListenPort()

            clientDoneMutex.lock()
            repeat (clientCount1.toInt()) {
                val client = getClientProcess() //ClientProcessTest(ClientAction.CONNECT_DISCONNECT)
                client.connect("localhost", port)
                assertFalse(client.isOpen())
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
                                                  getClientProcess: () -> ClientProcess) { //getClientSocket: () -> ClientToServerSocket) {
        val clients = ArrayList<ClientProcess>(clientCount.toInt())
        val clientDoneMutex = Mutex(locked = false)
        val clientCount = 50
        var counter = 0
        val server = launchServer(getServerSocket())
        val port = server.getListenPort()

        clientDoneMutex.lock()
        repeat(clientCount) {
            val client = getClientProcess() //ClientProcessTest(ClientAction.CONNECT_DISCONNECT)
            client.connect("localhost", port)
            assertFalse(client.isOpen())
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
    private suspend fun launchServer(server: TCPServer) : TCPServer {
        val handler = {exp: Exception -> (throw exp)}
        server.startServer()
        assertNotEquals(server.getListenPort(), server.port, "Server listen port is diferent")
        GlobalScope.launch {
            server.getClientConnection(handler)
            assertFalse(server.isOpen(), "Server socket is still open.")
        }
        assertTrue(server.isOpen(), "Server socket is not open.")
        return server
    }

}
