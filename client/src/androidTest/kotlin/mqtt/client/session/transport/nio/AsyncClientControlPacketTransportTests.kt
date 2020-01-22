package mqtt.client.session.transport.nio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

@ExperimentalCoroutinesApi
class AsyncClientControlPacketTransportTests {
//
//    lateinit var scope: CoroutineScope
//    private lateinit var transport : ClientControlPacketTransport
//    private val timeoutMs = 10_000L
//
//    @Before
//    fun setupConnection() {
//        scope = CoroutineScope(Job() + Dispatchers.Unconfined)
//        blockWithTimeout {  }
//        val connectionRequest = ConnectionRequest(clientId = "test${Random.nextInt()}", keepAliveSeconds = 2.toUShort())
//        scope.blockWithTimeout(75) {
//            transport = asyncClientTransport(connectionRequest, scope)
//            println("connack ${transport.open(60_000)}")
//        }
//    }

    @Test
    fun testAsyncConnection() = runBlockingTest {
        launch {
            delay(65_0000)
            println("done with delay")
        }
    }
}
