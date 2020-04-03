package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.allocateNewBuffer
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

class NewSocketTests {

    val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    @Test
    fun oneServerOneClient() = block {
        var port : UShort = 0u
        lateinit var server : ServerNew
        var client: ClientToServerSocket? = null
        val serverMutex = Mutex()
        val clientMutex = Mutex()

        serverMutex.lock()
        launch {
            val serverProcess = TestServerProcess()
            serverProcess.name = "Server-1"
            serverProcess.clientResponse = "Client-"
            server = ServerNew ("localhost", port, serverProcess)
            launchServer (serverMutex, port, server!!)
        }

        clientMutex.lock()
        serverMutex.lock()
        port = if (server != null) server.getListenPort() else 0u

        launch {
            client = asyncClientSocket()
            initiateClient(client!!, port)

            clientMutex.unlock()
        }

        clientMutex.lock()
        // both server & client are up & running.

        clientMessage (client!!, "Client-1", "Client-1:Server-1")
        client!!.close()
        server?.close()
    }

    @ExperimentalTime
    private suspend fun clientMessage(socket: ClientSocket, sendMsg: String, respMsg: String) {
        val timeout = 100.milliseconds
        val rbuffer = allocateNewBuffer(100.toUInt(), limits)
        val wbuffer = allocateNewBuffer(100.toUInt(), limits)

        wbuffer.writeUtf8String(sendMsg)
        socket.write(wbuffer, timeout)
        socket.read(rbuffer, timeout)

        assertEquals(respMsg, rbuffer.readMqttUtf8StringNotValidated().toString(), "Excepted message not received.")
    }

    @ExperimentalTime
    private suspend fun initiateClient(socket: ClientToServerSocket, port: UShort) {

        assertFalse(socket.isOpen(), "Client socket should not be open state")
        socket.open(100.seconds, port, "localhost")

        assertEquals(socket.remotePort(), port, "Remote port is not the as in connect request.")
        //println("client port #: ${client!!.localPort()}, ${client!!.remotePort()}")
        assertTrue(socket.isOpen(), "Connected to server, thus should be in open state")
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    private suspend fun launchServer(mutex: Mutex, port: UShort, server: ServerNew) {
        server.startServer()
        assertNotEquals(server.getListenPort(), port, "Server listen port is diferent")
        mutex.unlock()
        server.getClientConnection()

        assertFalse(server.isOpen(), "Server socket is still open.")
    }
}