package mqtt.socket

import jvmMain.kotlin.mqtt.socket.ssl.SSLClient
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.JvmBuffer
import mqtt.buffer.PlatformBuffer
import mqtt.buffer.allocateNewBuffer
import mqtt.socket.ssl.SSLManager
import mqtt.socket.ssl.SSLProcessor
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.text.Charsets.UTF_8
import kotlin.time.ExperimentalTime

class SSLTest {

    @ExperimentalUnsignedTypes
    val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 6000u
    }

    @Test
    fun oneClient() = block {
        val clientSocket = asyncClientSocket()

//        System.setProperty("javax.net.debug", "all")
        val manager:SSLManager = SSLManager("/Users/sbehera/cacerts", "changeit", "/Users/sbehera/cacerts", "changeit")
        println("oneClient: about to call getSSLclient")
        val client: SSLProcessor = manager.getSSLclient(clientSocket, "www.amazon.com", 443)
        val req: String = "GET / HTTP/1.1\r\nHost: www.amazon.com\r\naccept: text/html\r\n\r\n"
        //val req: String = "GET / HTTP/1.1\r\nHost: localhost:44330\r\naccept: text/html\r\n"

        val buf: PlatformBuffer = allocateNewBuffer(5000.toUInt(), limits)
        //buf.writeUtf8String(req)
        val ch: Charset = Charsets.UTF_8
        var bx: ByteBuffer = (buf as JvmBuffer).byteBuffer
        bx.put(req.toByteArray(ch))
        var x: Int = client.sslWrite(buf)
        println("message.write: $x, buf: $buf")

        x = client.sslRead(buf)
        println("message.read: $x, buf: $buf")
        println("initiated close:")
        client.initiateClose()
//        println("==>${buf.readMqttUtf8StringNotValidated().toString()}")

        bx = buf.byteBuffer
        println("==> buf: $bx")
        val ar: ByteArray = buf.readByteArray(426u)

        val str: String = String(ar, ch)
        println("==> $str")
     /*   val bufx: ByteBuffer = (buf as JvmBuffer).byteBuffer
 //       bufx.flip()
        println("message.read: bufx: $bufx")
        val charset: Charset = Charsets.UTF_8
        val ar: ByteArray = bufx.array()
        val ax: ByteArray = ByteArray(1)
        ax[0] = ar[0]
        val str: String = String(ax, charset)
        println("message.output: ${str}")
      */
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