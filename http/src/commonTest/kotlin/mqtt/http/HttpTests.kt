package mqtt.http

import mqtt.http.v1_1.HttpClient
import mqtt.socket.openClientSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class HttpTests {

    @Test
    fun httpRawSocket() = block {
        val client = openClientSocket(80u, hostname = "example.com")
        val request =
            """
GET / HTTP/1.1
Host: example.com
Connection: close

"""
        val bytesWritten = client.write(request)
        assertTrue { bytesWritten > 0 }
        val response = client.read().result
        client.close()
        assertTrue { response.contains("200 OK") }
        assertTrue { response.contains("HTTP") }
        assertTrue { response.contains("<html>") }
    }

    @Test
    fun http() = block {
        val response = HttpClient.request(HttpRequest("example.com"))
        assertEquals(response.statusCode, 200)
        assertTrue(response.body!!.contains("<html>"))
        assertTrue(response.body!!.contains("</html>"))
    }

}