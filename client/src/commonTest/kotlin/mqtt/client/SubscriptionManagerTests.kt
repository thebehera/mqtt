package mqtt.client

import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Name
import mqtt.wire.data.topic.TopicLevelNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubscriptionManagerTests {
    @Test
    fun registrationAndGetSubscription() {
        val subManger = SubscriptionManager()

        var cbFired = false
        val original = object : SubscriptionCallback<String> {
            override fun onMessageReceived(topic: Name, qos: QualityOfService, message: String) {
                cbFired = true
            }
        }
        subManger.register("hello", original)
        val sub = subManger.getSubscription<String>(TopicLevelNode.parse("hello")!!)
        assertEquals(original, sub)
        sub!!.onMessageReceived(Name("yolo"), QualityOfService.AT_MOST_ONCE, "")
        assertTrue(cbFired)
    }

    @Test
    fun deregistration() {
        val subManger = SubscriptionManager()

        val original = object : SubscriptionCallback<String> {
            override fun onMessageReceived(topic: Name, qos: QualityOfService, message: String) {}
        }
        subManger.register("hello", original)
        subManger.deregister("hello")
        assertTrue(subManger.rootNodeSubscriptions.isEmpty())
    }
}
