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
    fun http() = block {
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
        println(response)
        assertTrue { response.contains("200 OK") }
        assertTrue { response.contains("HTTP") }
        assertTrue { response.contains("<html>") }
    }

    @Test
    fun http2() = block {
        val headers = mapOf(Pair("Connection", "close")).toHeaders()
        val request = HttpRequest<String>(HttpMethod.GET, "example.com", 80u, "/", HttpVersion.v1_0, headers, null)
        val response = HttpClient.request(request)
        assertEquals(response.statusCode, 200)
        assertTrue(response.body?.contains("<html>") ?: false)
        assertTrue(response.body?.contains("</html>") ?: false)
    }

}