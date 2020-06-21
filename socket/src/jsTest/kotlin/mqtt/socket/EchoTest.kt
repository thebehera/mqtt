package mqtt.socket

import mqtt.buffer.JsBuffer
import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test

class EchoTest {

    @Test
    fun createServer() {
        val echoServerString = "Echo Server\r\n"
        val server = Net.createServer { socket ->
            val buffer = allocateNewBuffer(13u) as JsBuffer
            buffer.writeUtf8(echoServerString)
            socket.write(buffer.buffer) {}
            socket.pipe(socket)
        }
        val port = server.listen().address()!!.port
        println("Server running on port $port")
        server.close { }
    }


}