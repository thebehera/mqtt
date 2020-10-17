package mqtt.client

import mqtt.socket.asyncClientSocket
import kotlin.test.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class SimpleTests {

    @Test
    fun x() {
        val async = asyncClientSocket()
        println(async.toString())
    }
}