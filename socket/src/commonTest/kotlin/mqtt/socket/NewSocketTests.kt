package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.allocateNewBuffer
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

class NewSocketTests {

    @ExperimentalUnsignedTypes
    val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    @Test
    fun oneServerOneClient() = block {
        var port : UShort = 0u
        lateinit var server : ServerNew
        lateinit var client: ClientToServerSocket

        val serverProcess = TestServerProcess()
        serverProcess.name = "Server-1"
        serverProcess.clientResponse = "Client-"
        server = ServerNew ("localhost", port, serverProcess)
        launchServer (this, port, server)

        port = server.getListenPort()

        client = asyncClientSocket()
        initiateClient(client, port)

        clientMessage (client, "Client-1", "Client-1:Server-1")
        client.close()
        server.close()
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    @Test
    fun oneServerMultiClient() = block {
        var port: UShort = 0u
        val clientCount = 5000
        val serverProcess = TestServerProcess()
        serverProcess.name = "Server-1"
        serverProcess.clientResponse = "Client-"
        val server = ServerNew("localhost", port, serverProcess)
        launchServer(this, port, server)
        port = server.getListenPort()

        var closedConnections = 0
        val doneMutex = Mutex(true)
        repeat(clientCount) { i ->
            val client = asyncClientSocket()
            initiateClient(client, port)
            launch (Dispatchers.Default){
                clientMessage(client, "Client-$i", "Client-$i:Server-1")
                launch {
                    println("b==> $i")
                    client.close()
                    closedConnections++
                    if (closedConnections >= clientCount) {
                        server.close()
                        assertEquals(0, readStats(port, "CLOSE_WAIT").count(), "sockets found in close_wait state")
                        doneMutex.unlock()
                    }
                }
            }
        }
        val timeTook = measureTime {
            withTimeout(5000) {
                doneMutex.lock()
            }
        }
        println("Took $timeTook for $clientCount connections")
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    private suspend fun clientMessage(socket: ClientSocket, sendMsg: String, respMsg: String) {
        val timeout = 100.milliseconds
        val rbuffer = allocateNewBuffer(100.toUInt(), limits)
        val wbuffer = allocateNewBuffer(100.toUInt(), limits)

        try {
            wbuffer.writeUtf8String(sendMsg)
            socket.write(wbuffer, timeout)
            socket.read(rbuffer, timeout)
            println("a==>$sendMsg")
            val str: String = rbuffer.readMqttUtf8StringNotValidated().toString()
            assertEquals(respMsg, str, "Excepted message not received.")
        } catch (e: Exception) {
            println("NewSocketTest.clientMessage.exception: $sendMsg")
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    private suspend fun initiateClient(socket: ClientToServerSocket, port: UShort) {

        try {
            assertFalse(socket.isOpen(), "Client socket should not be open state")
            socket.open(100.seconds, port, "localhost")

            assertEquals(socket.remotePort(), port, "Remote port is not the as in connect request.")
            //println("client port #: ${client!!.localPort()}, ${client!!.remotePort()}")
            assertTrue(socket.isOpen(), "Connected to server, thus should be in open state")
        } catch (e: Exception) {
            println("NewSocketTest.initiateClient.exception: $port")
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    private suspend fun launchServer(scope: CoroutineScope, port: UShort, server: ServerNew) {
        server.startServer()
        assertNotEquals(server.getListenPort(), port, "Server listen port is diferent")
        scope.launch {
            server.getClientConnection()
            assertFalse(server.isOpen(), "Server socket is still open.")
        }
        assertTrue(server.isOpen(), "Server socket is not open.")
    }
}