package mqtt.socket

import kotlinx.coroutines.launch
import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalUnsignedTypes
@ExperimentalTime
class SimpleSocketTests {

    @Test
    fun httpRequest() = block {
        val client = openClientSocket(80u, hostname = "facebook.com")
        client.write("""
GET / HTTP/1.1
Host: facebook.com
Connection: close

"""
        )
        val response = client.read()
        val responseString = response.result.toString()
        assertTrue { responseString.contains("200 OK") }
        assertTrue { responseString.contains("HTTP") }
        assertTrue { responseString.contains("<html>") }
        client.close()
    }

    @Test
    fun serverEcho() = block {
        val server = asyncServerSocket()
        val clientToServer = asyncClientSocket()
        server.bind()

        val text = "yolo swag lyfestyle"
        val stringBuffer = allocateNewBuffer(text.length.toUInt())
        stringBuffer.writeUtf8(text)

        val serverPort = assertNotNull(server.port(), "No port number from server")
        launch {
            clientToServer.open(serverPort)
            clientToServer.write(stringBuffer)
        }
        val serverToClient = server.accept()
        val dataReceivedFromClient = serverToClient.read { buffer, bytesRead ->
            buffer.readUtf8(bytesRead.toUInt())
        }
        assertEquals(text, dataReceivedFromClient.result.toString())
        val serverToClientPort = assertNotNull(serverToClient.localPort())
        val clientToServerPort = assertNotNull(clientToServer.localPort())
        serverToClient.close()
        clientToServer.close()
        server.close()
        checkPort(serverToClientPort)
        checkPort(clientToServerPort)
        checkPort(serverPort)

    }

    @Test
    fun clientEcho() = block {
        val server = asyncServerSocket()
        val clientToServer = asyncClientSocket()
        server.bind()

        val text = "yolo swag lyfestyle"
        val stringBuffer = allocateNewBuffer(text.length.toUInt())
        stringBuffer.writeUtf8(text)

        val serverPort = assertNotNull(server.port(), "No port number from server")
        lateinit var serverToClient: ClientSocket
        launch {
            serverToClient = server.accept()
            serverToClient.write(stringBuffer)
        }
        clientToServer.open(serverPort)
        val dataReceivedFromServer = clientToServer.read { buffer, bytesRead ->
            buffer.readUtf8(bytesRead.toUInt())
        }
        assertEquals(text, dataReceivedFromServer.result.toString())
        val serverToClientPort = assertNotNull(serverToClient.localPort())
        val clientToServerPort = assertNotNull(clientToServer.localPort())
        serverToClient.close()
        clientToServer.close()
        server.close()
        checkPort(clientToServerPort)
        checkPort(serverToClientPort)
        checkPort(serverPort)
    }

    @ExperimentalUnsignedTypes
    private suspend fun checkPort(port: UShort) {
        val stats = readStats(port, "CLOSE_WAIT")
        if (stats.isNotEmpty()) {
            println("stats (${stats.count()}): $stats")
        }
        assertEquals(0, stats.count(), "Socket still in CLOSE_WAIT state found!")
    }
}