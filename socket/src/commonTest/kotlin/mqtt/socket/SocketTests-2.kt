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

class `SocketTests-2` {

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
        val clientCount = 6
        val serverProcess = ServerProcessTest(ServerAction.MQTTSTRING)
        serverProcess.name = "Server-x"
        serverProcess.clientResponse = "Client-"
        val server = TCPServer("localhost", port, serverProcess)
        launchServer(this, port, server)
        port = server.getListenPort()

        val doneMutex = Mutex(true)
        closeCounter.counter = 0
        repeat(clientCount) { i ->
            launch (Dispatchers.Default) {
                clientSetup(port, port, i, clientCount, doneMutex)
            }
        }
        doneMutex.lock()
        server.close()
        assertEquals(0, readStats(port, "CLOSE_WAIT").count(), "sockets found in close_wait state")
        doneMutex.unlock()
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    @Test
    fun twoServerMultiClient() = block {
        var port0: UShort = 0u
        var port1: UShort = 0u
        val clientCount = 8
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
        closeCounter.counter = 0
        repeat(clientCount) { i ->
            launch (Dispatchers.Default){
                clientSetup(port0, port1, i, clientCount, doneMutex)
            }
        }
        doneMutex.lock()
        server0.close()
        server1.close()
        assertEquals(0, readStats(port0, "CLOSE_WAIT").count(), "sockets found in close_wait state")
        assertEquals(0, readStats(port1, "CLOSE_WAIT").count(), "sockets found in close_wait state")
        doneMutex.unlock()
    }

    companion object closeCounter {
        var counter: Int = 0
        private val mux: Mutex = Mutex()
        suspend fun increment() {
            mux.lock()
            counter++
            mux.unlock()
        }
    }
    @ExperimentalTime
    private suspend fun clientSetup(port0: UShort, port1: UShort, counter: Int, clientCount:Int, mut: Mutex) {
        val xx:String = "Client-" + "$counter"
        val client = asyncClientSocket()

        if (counter % 2 == 0)
            initiateClient(client, port0)
        else
            initiateClient(client, port1)
        clientMessage(client, xx, "$xx:Server-x")
        client.close()
        closeCounter.increment()
        if (closeCounter.counter >= clientCount && mut.isLocked)
            mut.unlock()

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
            val str: String = rbuffer.readMqttUtf8StringNotValidated().toString()
//            println("=> $sendMsg, $respMsg, $str")
            assertEquals(respMsg, str, "Excepted message not received.")
        } catch (e: Exception) {
            assertTrue("".equals("clientMessage.exception: ${e.message}"))
 //           println("clientMessage.exception: $sendMsg, ${e.message}")
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
            assertTrue("".equals("initiateClient.exception: ${e.message}"))
//            println("initiateClient.exception: $port, ${e.message}")
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    private suspend fun launchServer(scope: CoroutineScope, port: UShort, server: TCPServer) {
        server.startServer()
        assertNotEquals(server.getListenPort(), port, "Server listen port is diferent")
        scope.launch {
            server.getClientConnection()
            assertFalse(server.isOpen(), "Server socket is still open.")
        }
        assertTrue(server.isOpen(), "Server socket is not open.")
    }
}