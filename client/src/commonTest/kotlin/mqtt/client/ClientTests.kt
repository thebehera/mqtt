@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.Runnable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import mqtt.client.connection.ConnectionParameters
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IPublishComplete
import mqtt.wire.data.QualityOfService.EXACTLY_ONCE
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientTests {

    @Test
    fun reconnectsAfterSocketConnectionFailure() {
        val request = ConnectionRequest(getClientId())
        val invalidBadPort = 1
        val params = ConnectionParameters("localhost", invalidBadPort, false, request,
                maxNumberOfRetries = 3)
        val client = MqttClient(params)
        val job = client.startAsync()
        block {
            job.await()
        }
        assertEquals(3, client.connectionCount)
    }

    @Test
    fun publishAutomaticallySent() {
        val request = ConnectionRequest(getClientId())
        val params = ConnectionParameters("localhost", 60000, false, request)
        val client = MqttClient(params)
        val mutex = Mutex(true)
        val publishMessageNumber = UShort.MAX_VALUE
        @Suppress("DeferredResultUnused")
        client.startAsync(Runnable {
            mutex.unlock()
        })
        block {
            mutex.lock()
            withTimeout(5000) {
                val pubCompMutex = Mutex(true)
                client.session.everyRecvMessageCallback = object : OnMessageReceivedCallback {
                    override fun onMessage(controlPacket: ControlPacket) {
                        if (controlPacket is IPublishComplete) {
                            assertEquals(publishMessageNumber, controlPacket.packetIdentifier)
                            pubCompMutex.unlock()
                        }
                    }
                }
                client.session.publish("yolo", EXACTLY_ONCE, publishMessageNumber)
                pubCompMutex.lock()
            }
        }
    }
}