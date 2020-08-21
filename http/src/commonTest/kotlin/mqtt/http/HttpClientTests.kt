package mqtt.http

import mqtt.buffer.toBuffer
import mqtt.socket.asyncClientSocket
import kotlin.test.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalUnsignedTypes
class HttpClientTests {
    @Test
    fun testExampleCom() = block {
        val socket = asyncClientSocket()
        socket.open(80u, hostname = "example.com")
        val headers =
            """GET / HTTP/1.1
Host: example.com
Connection: close
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36
Accept: text/html

"""
        socket.write(headers.toBuffer())
        val response = socket.read { platformBuffer, bytesRead ->
            platformBuffer.readUtf8(bytesRead.toUInt())
        }
        socket.close()
        println(response)
    }
}