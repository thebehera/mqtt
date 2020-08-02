package mqtt.socket

import kotlinx.coroutines.launch
import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class SimpleSocketTests {

    @Test
    fun echo() = block {
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
        serverToClient.close()
        clientToServer.close()
        server.close()
    }
}