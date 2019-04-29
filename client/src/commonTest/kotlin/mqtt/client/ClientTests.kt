@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import mqtt.client.connection.ConnectionParameters
import mqtt.client.transport.OnMessageReceivedCallback
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.IPublishAcknowledgment
import mqtt.wire.control.packet.IPublishComplete
import mqtt.wire.control.packet.getAndIncrementPacketIdentifier
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.QualityOfService.AT_LEAST_ONCE
import mqtt.wire.data.QualityOfService.EXACTLY_ONCE
import mqtt.wire4.control.packet.ConnectionRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

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

    fun createClient(): Pair<MqttClient, Deferred<Unit>> {
        val request = ConnectionRequest(getClientId())
        val params = ConnectionParameters("localhost", 60000, false, request)
        val client = MqttClient(params)
        val job = client.startAsyncWaitUntilFirstConnection()
        return Pair(client, job)
    }

    inline fun <reified T> blockUntilMessageReceived(topic: String, qos: QualityOfService,
                                                     publishMessageNumber: UShort = getAndIncrementPacketIdentifier(),
                                                     cb: OnMessageReceivedCallback? = null) {
        val (client, job) = createClient()
        blockWithTimeout(5000) {
            job.await()
            val pubCompMutex = Mutex(true)
            client.session.everyRecvMessageCallback = object : OnMessageReceivedCallback {
                override fun onMessage(controlPacket: ControlPacket) {
                    if (controlPacket is T) {
                        cb?.onMessage(controlPacket)
                        pubCompMutex.unlock()
                    }
                }
            }
            client.session.publish(topic, qos, publishMessageNumber)
            pubCompMutex.lock()
        }
    }

    @Test
    fun publishQos2PublishCompleteReceived() {
        val publishMessageNumber = UShort.MAX_VALUE
        blockUntilMessageReceived<IPublishComplete>("yolo", EXACTLY_ONCE, publishMessageNumber, cb = object : OnMessageReceivedCallback {
            override fun onMessage(controlPacket: ControlPacket) {
                if (controlPacket !is IPublishComplete) {
                    fail("invalid control packet type")
                }
                assertEquals(publishMessageNumber, controlPacket.packetIdentifier)
            }
        })
    }

    @Test
    fun publishQos1PublishAckReceived() {
        blockUntilMessageReceived<IPublishAcknowledgment>("yolo2", AT_LEAST_ONCE,
                cb = object : OnMessageReceivedCallback {
                    override fun onMessage(controlPacket: ControlPacket) {
                        if (controlPacket !is IPublishAcknowledgment) {
                            fail("invalid control packet type")
                        }
                    }
                })
    }
}