package mqtt.transport

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import mqtt.time.currentTimestampMs
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

const val clientCount = 128L

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class SocketTests {


    @Test(timeout = clientCount * 2)
    fun test() = runBlocking {
        var count = 0
        val clientsMap = HashMap<UShort, SuspendCloseable>()
        val server = asyncServerSocket(this, 10.milliseconds, 10.milliseconds)
        server.bind()
        val mutex = Mutex(true)
        launch {
            server.listen().collect {
                ++count
                println("$count")
                if (count >= clientCount) {
                    mutex.unlock()
                }
                it.close()
            }
        }
        repeat(clientCount.toInt()) {
            val client = asyncClientSocket(this, 10.milliseconds, 10.milliseconds)
            client.open(port = server.port()!!)
            val clientPort = client.port()
            if (clientPort != null) {
                clientsMap[clientPort] = client
            }
        }

        mutex.lock()
        clientsMap.values.forEach { it.close() }
        server.close()
        println("${currentTimestampMs()} server close $clientCount clients tested")
        assert(clientCount == count.toLong())
    }
}