package mqtt.socket

import jvmMain.kotlin.mqtt.socket.ssl.SSLClient
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.PlatformBuffer
import mqtt.buffer.allocateNewBuffer
import mqtt.socket.ssl.SSLManager
import mqtt.socket.ssl.SSLProcessor
import org.junit.Test
import kotlin.time.ExperimentalTime

class SSLTest {

    @ExperimentalUnsignedTypes
    val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    @Test
    fun oneClient() = block {
        val clientSocket = asyncClientSocket()

//        System.setProperty("javax.net.debug", "all")
        val manager:SSLManager = SSLManager("/Users/sbehera/cacerts", "changeit", "/Users/sbehera/cacerts", "changeit")
        println("oneClient: about to call getSSLclient")
        val client: SSLProcessor = manager.getSSLclient(clientSocket, "www.paypal.com", 443)
        client.initiateClose()
    }
    /*
    @ExperimentalUnsignedTypes
    @ExperimentalTime
    @Test
    fun oneClient() = block {
        val clientSocket = asyncClientSocket()
        val client: SSLClient = SSLClient()

       // System.setProperty("javax.net.debug", "all")
        client.open(clientSocket, "www.google.com", 443u)
     //   client.open(clientSocket, "localhost", 44330u)

        val buf: PlatformBuffer = allocateNewBuffer(5000.toUInt(), limits)

        //val abc: String = "GET /business/login/ HTTP/1.1\r\nHost: controlcenter.centurylink.com\r\n\r\n"
        val abc: String = "GET / HTTP/1.1\r\nHost: www.google.com\r\n\r\n"
        buf.writeUtf8String(abc)
     //   buf.resetForRead()
        println("buf==>${buf}")
        var x: Int = client.sslWrite(buf)
        println("==> $x")
        buf.resetForWrite()
        x = client.sslRead(buf)
        buf.resetForRead()
        println("==> $x; ${buf}")
        println("${buf.readMqttUtf8StringNotValidated().toString()}")
        client.initiateClose()
    }
     */
}