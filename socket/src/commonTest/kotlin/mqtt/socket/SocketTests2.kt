@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.allocateNewBuffer
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds


class SocketTests2 {

    @ExperimentalUnsignedTypes
    val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    @Test
    fun oneServerOneClient() = block {
        var port : UShort = 0u
        lateinit var server : TCPServer
        lateinit var client: ClientToServerSocket

        val serverProcess = ServerProcessTest(ServerAction.MQTTSTRING)
        serverProcess.name = "Server-1"
        serverProcess.clientResponse = "Client-"
        server = TCPServer ("localhost", port, serverProcess)
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
        val clientCount = 25
        val serverProcess = ServerProcessTest(ServerAction.MQTTSTRING)
        serverProcess.name = "Server-x"
        serverProcess.clientResponse = "Client-"
        val server = TCPServer("localhost", port, serverProcess)
        launchServer(this, port, server)
        port = server.getListenPort()

        val doneMutex = Mutex(true)
        counter = 0
        repeat(clientCount) { i ->
            launch (Dispatchers.Default) {
                clientSetup(port, port, i, clientCount, doneMutex)
            }
        }
        doneMutex.lock()
        server.close()
//        assertEquals(0, readStats(port, "CLOSE_WAIT").count(), "sockets found in close_wait state")
        doneMutex.unlock()
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    @Test
    fun twoServerMultiClient() = block {
        var port0: UShort = 0u
        var port1: UShort = 0u
        val clientCount = 10
        val serverProcess = ServerProcessTest(ServerAction.MQTTSTRING)
        serverProcess.name = "Server-x"
        serverProcess.clientResponse = "Client-"

        val server0 = TCPServer("localhost", port0, serverProcess)
        val server1 = TCPServer("localhost", port1, serverProcess)
        launchServer(this, port0, server0)
        launchServer(this, port1, server1)
        port0 = server0.getListenPort()
        port1 = server1.getListenPort()

        val doneMutex = Mutex(true)
        counter = 0
        repeat(clientCount) { i ->
            launch (Dispatchers.Default){
                clientSetup(port0, port1, i, clientCount, doneMutex)
            }
        }
        doneMutex.lock()
        server0.close()
        server1.close()
//        assertEquals(0, readStats(port0, "CLOSE_WAIT").count(), "sockets found in close_wait state")
//        assertEquals(0, readStats(port1, "CLOSE_WAIT").count(), "sockets found in close_wait state")
        doneMutex.unlock()
    }

    companion object closeCounter {
        var counter = 0
        private val mux: Mutex = Mutex()
        suspend fun increment() {
            mux.lock()
            counter++
            mux.unlock()
        }
    }

    @ExperimentalTime
    private suspend fun clientSetup(port0: UShort, port1: UShort, counter: Int, clientCount:Int, mut: Mutex) {
        val sendMsg:String = "Client-" + "$counter"
        val client = asyncClientSocket()

        if (counter % 2 == 0)
            initiateClient(client, port0)
        else
            initiateClient(client, port1)
        clientMessage(client, sendMsg, "$sendMsg:Server-x")
        client.close()
        increment()
        if (closeCounter.counter >= clientCount && mut.isLocked)
            mut.unlock()

    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    private suspend fun clientMessage(socket: ClientSocket, sendMsg: String, respMsg: String) {
        val timeout = 100.milliseconds
        val wbuffer = allocateNewBuffer(100.toUInt(), limits)
        wbuffer.writeMqttUtf8String(sendMsg)
        socket.write(wbuffer, timeout)
        val str = socket.readTyped(timeout) { rbuffer ->
            rbuffer.readMqttUtf8StringNotValidated().toString()
        }
        assertEquals(respMsg, str, "Excepted message not received.")
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    private suspend fun initiateClient(socket: ClientToServerSocket, port: UShort) {

        try {
            assertFalse(socket.isOpen(), "Client socket should not be open state")
            socket.open(100.seconds, port, "localhost")

            assertEquals(socket.remotePort(), port, "Remote port is not the as in connect request.")

            assertTrue(socket.isOpen(), "Connected to server, thus should be in open state")
        } catch (e: Exception) {
            assertTrue("".equals("initiateClient.exception: ${e.message}"))
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    private suspend fun launchServer(scope: CoroutineScope, port: UShort, server: TCPServer) {
        val handler = {exp: Exception -> (throw exp)}
        server.startServer()
        assertNotEquals(server.getListenPort(), port, "Server listen port is diferent")
        scope.launch {
            server.getClientConnection(handler)
            assertFalse(server.isOpen(), "Server socket is still open.")
        }
        assertTrue(server.isOpen(), "Server socket is not open.")
    }
}