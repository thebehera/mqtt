package mqtt.socket

import jvmMain.kotlin.mqtt.socket.ssl.SSLClient
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.PlatformBuffer
import mqtt.buffer.allocateNewBuffer
import org.junit.Test
import kotlin.time.ExperimentalTime

class SSLTest {

    @ExperimentalUnsignedTypes
    val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    @ExperimentalUnsignedTypes
    @ExperimentalTime
    @Test
    fun oneClient() = block {
        val clientSocket = asyncClientSocket()
        val client: SSLClient = SSLClient()

        System.setProperty("javax.net.debug", "all")
        client.open(clientSocket, "www.google.com", 443u)
     //   client.open(clientSocket, "localhost", 44330u)

        val buf: PlatformBuffer = allocateNewBuffer(5000.toUInt(), limits)

        val x: Int = client.sslRead(buf)
        println("==> $x")
        client.initiateClose()
    }
}