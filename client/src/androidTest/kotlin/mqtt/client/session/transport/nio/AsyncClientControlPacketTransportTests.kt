package mqtt.client.session.transport.nio

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import mqtt.client.blockWithTimeout
import mqtt.client.session.transport.MockTransportServer
import mqtt.connection.ClientControlPacketTransport
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire4.control.packet.ConnectionAcknowledgment
import mqtt.wire4.control.packet.ConnectionRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
class AsyncClientControlPacketTransportTests {

    lateinit var singleThreadExecutor: ExecutorService
    lateinit var singleThreadScope: CoroutineScope
    lateinit var singleThreadProvider: AsynchronousChannelGroup

    @Before
    fun reset() {
        singleThreadExecutor = Executors.newSingleThreadExecutor()
        singleThreadScope = CoroutineScope(singleThreadExecutor.asCoroutineDispatcher())
        singleThreadProvider = AsynchronousChannelGroup.withThreadPool(singleThreadExecutor)!!
    }

    @InternalCoroutinesApi
    @ExperimentalTime
    @Test(timeout = 6000)
    fun testAsyncConnection() {
        singleThreadScope.blockWithTimeout(6000) {
            val server = MockTransportServer(this, 3000, singleThreadProvider)
            val connectionRequest = ConnectionRequest(
                clientId = "test${Random.nextInt()}",
                keepAliveSeconds = 5.toUShort()
            )

            val clientTransport = asyncClientTransport(this, connectionRequest, singleThreadProvider)
            val flow = server.listen()
            launch {
                flow.collect {
                    println("mock incoming ${it.connectionRequest}")
                    it.outboundChannel.send(
                        ConnectionAcknowledgment(
                            false,
                            ConnectionAcknowledgment.VariableHeader.ReturnCode.CONNECTION_ACCEPTED
                        )
                    )
                }
            }
            val address = server.localAddress!!
            val connack = clientTransport.open(address.port.toUShort())
            assert(connack.isSuccessful) { "incorrect connack message" }
            clientTransport.suspendClose()
            disconnect(clientTransport)
            server.close()
        }
    }


    @ExperimentalTime
    fun disconnect(transport: ClientControlPacketTransport) {
        val completedWrite = transport.completedWrite
        if (completedWrite != null) {
            assert(completedWrite.isClosedForSend)
        }
        assertFalse(transport.isOpen())
        assertNull(transport.assignedPort(), "Leaked socket")
        assert(transport.outboundChannel.isClosedForSend)
        assert((transport.inboxChannel as Channel<ControlPacket>).isClosedForSend)
    }

    @After
    fun close() {
        singleThreadProvider.shutdownNow()
        assertTrue(singleThreadExecutor.shutdownNow().isNullOrEmpty())
        assertTrue(singleThreadProvider.awaitTermination(5.toLong(), TimeUnit.SECONDS))
        assertTrue(singleThreadExecutor.awaitTermination(1.toLong(), TimeUnit.SECONDS))
    }
}
