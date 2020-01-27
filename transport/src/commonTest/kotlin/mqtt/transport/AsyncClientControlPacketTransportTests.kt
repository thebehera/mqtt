package mqtt.transport

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mqtt.connection.ClientControlPacketTransport
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IPingRequest
import mqtt.wire4.control.packet.ConnectionAcknowledgment
import mqtt.wire4.control.packet.ConnectionRequest
import mqtt.wire4.control.packet.PingRequest
import kotlin.math.max
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
class AsyncClientControlPacketTransportTests {


    @InternalCoroutinesApi
    @ExperimentalTime
    @Test
    fun testAsyncConnection() {
        val timeout = 6000
        blockWithTimeout(timeout.toLong()) {
            val server = aServer(this, 3000)
            val connectionRequest = ConnectionRequest(
                clientId = "test${Random.nextInt()}",
                keepAliveSeconds = 1.toUShort()
            )
            val keepAliveCount = max(((timeout / 1000) / connectionRequest.keepAliveTimeoutSeconds.toInt()) - 1, 1)
            val clientTransport = aMqttClient(this, connectionRequest, 12000, null)
            val flow = server.listen()
            val port = server.port()!!
            val server2ClientMutex = Mutex(true)
            launch {
                flow.collect { server2ClientTransport ->
                    println("mock incoming ${server2ClientTransport.connectionRequest}")
                    server2ClientTransport.outboundChannel.send(
                        ConnectionAcknowledgment(
                            false,
                            ConnectionAcknowledgment.VariableHeader.ReturnCode.CONNECTION_ACCEPTED
                        )
                    )
                    server2ClientTransport.incomingControlPackets.filterIsInstance<IPingRequest>().take(keepAliveCount)
                        .toList()
                    server2ClientMutex.unlock()
                }
            }
            val connack = clientTransport.open(port)
            assertTrue(connack.isSuccessful, "incorrect connack message")
            repeat(keepAliveCount) {
                clientTransport.outboundChannel.send(PingRequest)
            }
            server2ClientMutex.lock()
            clientTransport.suspendClose()
            clientTransport.close()
            server.close()
            disconnect(clientTransport)
        }
    }

    @ExperimentalTime
    fun disconnect(transport: ClientControlPacketTransport) {
        val completedWrite = transport.completedWrite
        if (completedWrite != null) {
            assertTrue(completedWrite.isClosedForSend)
        }
        assertFalse(transport.isOpen())
        assertNull(transport.assignedPort(), "Leaked socket")
        assertTrue(transport.outboundChannel.isClosedForSend)
        assertTrue((transport.inboxChannel as Channel<ControlPacket>).isClosedForSend)
    }

}
