package mqtt.wire.data.topic

import mqtt.wire.data.MqttUtf8String
import kotlin.test.Test
import kotlin.test.assertTrue

class TopicLevelNodeTests {
    @Test
    fun singleLevelWildcardInBetweeenTopicLevelsExactTopicMatches() {
        val topic1 = TopicLevelNode.from(MqttUtf8String("sport/+/p1"))
        val topic2 = TopicLevelNode.from(MqttUtf8String("sport/+/p1"))
        assertTrue(topic1.matchesTopic(topic2))
    }

    @Test
    fun singleLevelWildcardInBetweeenTopicLevels() {
        val topic = TopicLevelNode.from(MqttUtf8String("sport/+/p1")) + TopicLevelNode.from(MqttUtf8String("sport/+/p2"))
        assertTrue(topic.matchesTopic(TopicLevelNode.parse("sport/yolo/p1")!!))
    }


    @Test
    fun addTopicStructures() {
        var topic1 = TopicLevelNode.from(MqttUtf8String("sport/+/p1"))
        topic1 += TopicLevelNode.from(MqttUtf8String("sport/+/p2"))
        topic1.toString()
    }
}
