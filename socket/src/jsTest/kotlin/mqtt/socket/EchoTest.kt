package mqtt.socket

import kotlinx.coroutines.sync.Mutex
import mqtt.buffer.JsBuffer
import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class EchoTest {

    @Test
    fun echoClientServer() = block {
        val clientSendString = "hello\r\n"
        val clientBuffer = allocateNewBuffer(7u) as JsBuffer
        clientBuffer.writeUtf8(clientSendString)
        val server = Net.createServer { socket ->
            socket.pipe(socket)
        }
        val serverPort = server.listen().address()!!.port
        var client: Socket? = null
        client = Net.connect(tcpOptions(serverPort)) {
            client!!.write(clientBuffer.buffer) {}
        }
        val mutext = Mutex(true)
        client.on("data") { data ->
            assertEquals(data.toString(), clientSendString)
            client.end { }
            server.close {
                mutext.unlock()
            }
        }
        mutext.lock()
    }


}