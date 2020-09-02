package mqtt.socket

import mqtt.buffer.BufferPool
import mqtt.buffer.PlatformBuffer
import mqtt.socket.nio2.AsyncClientSocket
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.milliseconds

class SslClientTest {

    @Test
    fun http1() = block {
        val pool = BufferPool()
        val timeout = 500.milliseconds
        val clientSocket = AsyncClientSocket(pool = pool, ssl = true)
        val text = "GET /business/login/ HTTP/1.1\r\nHost: controlcenter.centurylink.com\r\n\r\n"

        pool.borrowSuspend(text.length.toUInt()) { clientBuffer: PlatformBuffer ->
            clientBuffer.writeUtf8(text)

            clientSocket.open(443u, timeout, hostname = "controlcenter.centurylink.com")
            assertTrue(clientSocket.isOpen(), "Socket is not open")
            assertEquals(70, clientSocket.write(clientBuffer, timeout), "All bytes were not written")

            val dataFromServer = clientSocket.read(timeout) {buffer: PlatformBuffer, size: Int ->
                buffer.readUtf8(size.toUInt())
            }
            assertEquals("HTTP/1.1 301", dataFromServer.result.substring(0,12), "Received data does not match")
            assertEquals(519, dataFromServer.bytesRead, "Incorrect # of bytes read")

//            println("$dataFromServer")
        }
        clientSocket.close()

    }
}