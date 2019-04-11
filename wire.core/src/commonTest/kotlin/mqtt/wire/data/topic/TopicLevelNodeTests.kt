package mqtt.wire.data.topic

import mqtt.wire.data.MqttUtf8String
import kotlin.test.*

class TopicLevelNodeTests {
    @Test
    fun singleLevelWildcardInBetweeenTopicLevelsExactTopicMatches() {
        val topic1 = TopicLevelNode.from(MqttUtf8String("sport/+/p1"))
        val topic2 = TopicLevelNode.from(MqttUtf8String("sport/+/p1"))
        assertTrue(topic1.matchesTopic(topic2))
    }

    @Test
    fun singleLevelWildcardInBetweeenTopicLevels() {
        val topic = TopicLevelNode.from(MqttUtf8String("sport/+/p1"))
        topic += TopicLevelNode.from(MqttUtf8String("sport/+/p2"))
        assertTrue(topic.matchesTopic(TopicLevelNode.parse("sport/yolo/p1")!!))
    }

    @Test
    fun addTopicStructures() {
        val mergedTopic = TopicLevelNode.from(MqttUtf8String("sport/+/p1"))
        mergedTopic += TopicLevelNode.from(MqttUtf8String("sport/+/p2"))
        assertTrue(mergedTopic.matchesTopic(TopicLevelNode.parse("sport/+/p1")!!))
        assertTrue(mergedTopic.matchesTopic(TopicLevelNode.parse("sport/+/p2")!!))
    }

    @Test
    fun removeTopicStructures() {
        val mergedTopic = TopicLevelNode.from(MqttUtf8String("sport/+/p1"))
        mergedTopic += TopicLevelNode.from(MqttUtf8String("sport/+/p2"))

        mergedTopic -= TopicLevelNode.from(MqttUtf8String("sport/+/p2"))
        assertEquals(mergedTopic.children.values.first().children.size, 1)
        assertEquals(mergedTopic.getAllBottomLevelChildren().first().toString(), "sport/+/p1")
    }

    @Test
    fun multiLevelWildcardNonNormative1() {
        val exampleTopic = TopicLevelNode.parse("sport/tennis/player1/#") ?: fail("failed to build topic")
        assertTrue(exampleTopic.matchesTopic(TopicLevelNode.parse("sport/tennis/player1")!!))
    }

    @Test
    fun multiLevelWildcardNonNormative2() {
        val exampleTopic = TopicLevelNode.parse("sport/tennis/player1/#") ?: fail("failed to build topic")
        assertTrue(exampleTopic.matchesTopic(TopicLevelNode.parse("sport/tennis/player1/ranking")!!))
    }

    @Test
    fun multiLevelWildcardNonNormative3() {
        val exampleTopic = TopicLevelNode.parse("sport/tennis/player1/#") ?: fail("failed to build topic")
        assertTrue(exampleTopic.matchesTopic(TopicLevelNode.parse("sport/tennis/player1/score/wimbledon")!!))
    }

    @Test
    fun multiLevelWildcardNonNormative4ParentLevel() {
        val exampleTopic = TopicLevelNode.parse("sport/#") ?: fail("failed to build topic")
        assertTrue(exampleTopic.matchesTopic(TopicLevelNode.parse("sport")!!))
    }

    @Test
    fun multiLevelWildcardNonNormative5EveryMessage() {
        val exampleTopic = TopicLevelNode.parse("#") ?: fail("failed to build topic")
        assertTrue(exampleTopic.matchesTopic(TopicLevelNode.parse("sport/tennis/player1/score/wimbledon")!!))
    }

    @Test
    fun multiLevelWildcardNonNormative6() {
        TopicLevelNode.parse("sport/tennis/#") ?: fail("failed to build topic")
    }

    @Test
    fun multiLevelWildcardNonNormative7() {
        try {
            TopicLevelNode.parse("sport/tennis#")
            fail("invalid topic name, should of failed")
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun multiLevelWildcardNonNormative8Invalid() {
        val exampleTopic = TopicLevelNode.parse("sport/tennis/player1/#") ?: fail("failed to build topic")
        assertTrue(exampleTopic.matchesTopic(TopicLevelNode.parse("sport/tennis/player1/score/wimbledon")!!))
    }

    @Test
    fun singleLevelWildcardNonNormative() {
        val example = TopicLevelNode.parse("sport/tennis/+") ?: fail("failed to build topic")
        assertTrue(example.matchesTopic(TopicLevelNode.parse("sport/tennis/player1")!!))
    }

    @Test
    fun singleLevelWildcardNonNormative1() {
        val example = TopicLevelNode.parse("sport/tennis/+") ?: fail("failed to build topic")
        assertTrue(example.matchesTopic(TopicLevelNode.parse("sport/tennis/player2")!!))
    }

    @Test
    fun singleLevelWildcardNonNormative3Negative() {
        val example = TopicLevelNode.parse("sport/tennis/+") ?: fail("failed to build topic")
        assertFalse(example.matchesTopic(TopicLevelNode.parse("sport/tennis/player1/ranking")!!))
    }

    @Test
    fun singleLevelWildcardMatchesOnlyASingleLevelNegative() {
        val example = TopicLevelNode.parse("sport/+") ?: fail("failed to build topic")
        assertFalse(example.matchesTopic(TopicLevelNode.parse("sport")!!))
    }

    @Test
    fun singleLevelWildcardMatchesOnlyASingleLevelPositive() {
        val example = TopicLevelNode.parse("sport/+") ?: fail("failed to build topic")
        assertTrue(example.matchesTopic(TopicLevelNode.parse("sport/")!!))
    }

    @Test
    fun singleLevelWildcardNonNormativeSimplePlusValid() {
        TopicLevelNode.parse("+") ?: fail("failed to build topic")
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet2() {
        TopicLevelNode.parse("+/tennis/#") ?: fail("failed to build topic")
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet3Negative() {
        try {
            TopicLevelNode.parse("sport+")
            fail("failed to build topic")
        } catch (e: IllegalArgumentException) {
        }
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet4() {
        TopicLevelNode.parse("sport/+/player1") ?: fail("failed to build topic")
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet5Match1() {
        val example = TopicLevelNode.parse("/finance") ?: fail("failed to build topic")
        assertTrue(example.matchesTopic(TopicLevelNode.parse("+/+")!!))
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet5Match2() {
        val example = TopicLevelNode.parse("/finance") ?: fail("failed to build topic")
        assertTrue(example.matchesTopic(TopicLevelNode.parse("/+")!!))
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet5Match3Negative() {
        val example = TopicLevelNode.parse("/finance") ?: fail("failed to build topic")
        assertFalse(example.matchesTopic(TopicLevelNode.parse("+")!!))
    }
}
