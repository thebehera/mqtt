package mqtt.socket

import kotlinx.coroutines.sync.Mutex
import mqtt.buffer.JsBuffer
import mqtt.buffer.allocateNewBuffer
import org.khronos.webgl.Uint8Array
import kotlin.test.Test
import kotlin.test.assertEquals

class EchoTest {

    @Test
    fun echoClientServer() = block {
        val clientSendString = "hello\r\n"
        val clientBuffer = allocateNewBuffer(clientSendString.length.toUInt()) as JsBuffer
        clientBuffer.writeUtf8(clientSendString)
        val server = Net.createServer { socket ->
            socket.pipe(socket)
        }
        val serverPort = server.listen().address()!!.port
        var client: Socket? = null

        val arrayPlatformBufferMap = HashMap<Uint8Array, JsBuffer>()
        val onRead = OnRead({
            val buffer = allocateNewBuffer(100u) as JsBuffer
            arrayPlatformBufferMap[buffer.buffer] = buffer
            buffer.buffer
        }, { bytesRead, buffer ->
            val jsBuffer = arrayPlatformBufferMap[buffer]!!
            jsBuffer.setPosition(0)
            jsBuffer.setLimit(bytesRead)
            assertEquals(clientSendString, jsBuffer.readUtf8(bytesRead.toUInt()).toString())
            true
        })
        val mutex = Mutex(true)
        val options = tcpOptions(serverPort, onread = onRead)
        console.log(options)
        client = Net.connect(options) {
            client!!.on("error") { err ->
                error(err.toString())
            }
            client!!.write(clientBuffer.buffer) {
                client!!.end {
                    server.close {
                        mutex.unlock()
                    }
                }
            }
        }
        println("yo")
        mutex.lock()
        println("done")
    }


}