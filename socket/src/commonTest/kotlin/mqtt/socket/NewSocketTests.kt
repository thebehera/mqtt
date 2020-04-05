package mqtt.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.allocateNewBuffer
import kotlin.test.*
import kotlin.time.ExperimentalTime
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
        lateinit var server: ServerNew
//        val client = mutableListOf<ClientToServerSocket>()
        val clientCount: Int = 100
        val mut: Mutex = Mutex()
        var c: Int = 0

        val serverProcess = TestServerProcess()
        serverProcess.name = "Server-1"
        serverProcess.clientResponse = "Client-"
        server = ServerNew("localhost", port, serverProcess)
        launchServer(this, port, server)


        port = server.getListenPort()

        repeat(clientCount) { i ->
            launch {
                val client: ClientToServerSocket = asyncClientSocket()
                //client.add(asyncClientSocket())
                initiateClient(client, port)
                clientMessage(client, "Client-$i", "Client-$i:Server-1")
                client.close()
                mut.lock()
                c++
                mut.unlock()
                if (c >= clientCount - 1)
                    server.close()
            }
        }
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    private suspend fun clientMessage(socket: ClientSocket, sendMsg: String, respMsg: String) {
        val timeout = 100.milliseconds
        val rbuffer = allocateNewBuffer(100.toUInt(), limits)
        val wbuffer = allocateNewBuffer(100.toUInt(), limits)

        wbuffer.writeUtf8String(sendMsg)
        socket.write(wbuffer, timeout)
        socket.read(rbuffer, timeout)

        val str:String = rbuffer.readMqttUtf8StringNotValidated().toString()
        assertEquals(respMsg, str, "Excepted message not received.")
    }

    @ExperimentalUnsignedTypes
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