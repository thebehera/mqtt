package mqtt.socket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class SimpleSocketTests {

    @Test
    fun httpRequest() = block {
        val client = openClientSocket(80u, hostname = "example.com")
        val request =
            """
GET / HTTP/1.1
Host: example.com
Connection: close

"""
        val bytesWritten = client.write(request)
        assertTrue { bytesWritten > 0 }
        val response = client.read().result
        assertTrue { response.contains("200 OK") }
        assertTrue { response.contains("HTTP") }
        assertTrue { response.contains("<html>") }
        client.close()
    }

    @Test
    fun serverEcho() = block {
        val server = asyncServerSocket()
        server.bind()
        val text = "yolo swag lyfestyle"
        val serverPort = assertNotNull(server.port(), "No port number from server")
        val clientToServer = asyncClientSocket()
        launch(Dispatchers.Unconfined) {
            clientToServer.open(serverPort)
            clientToServer.write(text)
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
        server.bind()
        val clientToServer = asyncClientSocket()
        val text = "yolo swag lyfestyle"
        val serverPort = assertNotNull(server.port(), "No port number from server")
        lateinit var serverToClient: ClientSocket
        launch {
            serverToClient = server.accept()
            serverToClient.write(text)
        }
        clientToServer.open(serverPort)
        val dataReceivedFromServer = withContext(Dispatchers.Default) {
            clientToServer.read { buffer, bytesRead -> buffer.readUtf8(bytesRead.toUInt()) }
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