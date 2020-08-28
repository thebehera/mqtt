package mqtt.socket

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mqtt.buffer.BufferPool
import mqtt.buffer.PlatformBuffer
import mqtt.buffer.allocateNewBuffer
import kotlin.jvm.JvmField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

class SslTest {

    @ExperimentalTime
    @Test
    fun client1 () = block {
        val clientToServer = asyncClientSocket(ssl = true)
        val text = "GET /business/login/ HTTP/1.1\r\nHost: controlcenter.centurylink.com\r\n\r\n"
        val stringBuffer = allocateNewBuffer(text.length.toUInt())

        stringBuffer.writeUtf8(text)

//        println("==>$text")
        clientToServer.open(443u, 5000.milliseconds, "controlcenter.centurylink.com")
        assertTrue(clientToServer.isOpen(), "Socket is not open")
        assertEquals(70, clientToServer.write(stringBuffer, 5000.milliseconds), "All bytes were not written")
        var dataReceivedFromClient = clientToServer.read(5000.milliseconds, {buffer: PlatformBuffer, size: Int ->
//               println("client1. size: $size")
                buffer.readUtf8(size.toUInt())
            })

        assertEquals("HTTP/1.1 301", dataReceivedFromClient.result.substring(0,12), "Received data does not match")
        assertEquals(519, dataReceivedFromClient.bytesRead, "Incorrect # of bytes read")
//        println("==client1==> ${dataReceivedFromClient}")

        clientToServer.close()
        assertFalse(clientToServer.isOpen(), "Socket is not closed")
    }

}