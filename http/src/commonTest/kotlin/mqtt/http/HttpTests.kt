package mqtt.http

import mqtt.http.v1_1.HttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class HttpTests {

    @Test
    fun http() = block {
        val response = HttpClient.request(HttpRequest("example.com"))
        assertEquals(response.statusCode, 200)
        assertTrue(response.body!!.contains("<html>"))
        assertTrue(response.body!!.contains("</html>"))
    }

}