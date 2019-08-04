@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.data.topic

import mqtt.wire.ProtocolError
import mqtt.wire.data.MqttUtf8String
import kotlin.test.*

class NodeTests {

    @Test
    fun subscriptionMatch() {
        val sub1 = Node.parse("hello/meow")
        val topic2 = Name("hello")
        val matchedNode = sub1.find(topic2)
        assertEquals("hello", matchedNode?.level?.value)
        val topic3 = Name("hello/meow")
        val matchedNode3 = sub1.find(topic3)
        assertEquals("meow", matchedNode3?.level?.value)
    }


    @Test
    fun twoSubscriptions() {
        val sub1 = Node.parse("hello/meow")
        val sub1Root = sub1.root()
        sub1Root += Node.parse("hello/meow2").root()
        sub1Root += Node.parse("hello/meow3").root()
        sub1Root += Node.parse("hello/+").root()
        val allChildren = sub1Root.allChildren()
        assertEquals(4, allChildren.size)
    }

//    @Test
//    fun validate() {
//        val childNode = Node.parse("/yolo/swag")
//        installSerializer(object : MqttSerializable<UInt> {
//            override fun serialize(obj: UInt) = buildPacket { writeUInt(obj) }
//
//            override fun deserialize(buffer: ByteReadPacket) = buffer.readUInt()
//
//        })
//
//        childNode.addCallback(CallbackTypeReference(object : SubscriptionCallback<String> {
//            override fun onMessageReceived(topic: Name, qos: QualityOfService, message: String?) {
//                assertEquals("hello", topic.topic)
//                assertEquals(qos, QualityOfService.AT_MOST_ONCE)
//                assertEquals("meow", message)
//            }
//        }, String::class))
//        childNode.handlePublish(PublishMessage("hello", "meow"))
//
//        val childNode2 = Node.parse("/yolo/swag")
//        childNode2.addCallback(CallbackTypeReference(object : SubscriptionCallback<UInt> {
//            override fun onMessageReceived(topic: Name, qos: QualityOfService, message: UInt?) {
//                assertEquals("/yolo/swag", topic.topic)
//                assertEquals(qos, QualityOfService.AT_MOST_ONCE)
//                assertEquals(5.toUInt(), message)
//            }
//        }, UInt::class))
//        childNode2.handlePublish(PublishMessage("/yolo/swag", findSerializer<UInt>()!!.serialize(5.toUInt())))
//    }

    @Test
    fun singleLevelWildcardInBetweeenTopicLevelsExactTopicMatches() {
        val topic1 = Node.from(MqttUtf8String("sport/+/p1"))
        val topic2 = Node.from(MqttUtf8String("sport/+/p1"))
        assertTrue(topic1.matchesTopic(topic2))
    }

    @Test
    fun singleLevelWildcardInBetweeenTopicLevels() {
        val topic = Node.from(MqttUtf8String("sport/+/p1"))
        topic += Node.from(MqttUtf8String("sport/+/p2"))
        topic += Node.from(MqttUtf8String("sportw/+/p2"))
        assertTrue(topic.matchesTopic(Node.parse("sport/yolo/p1")))
    }

    @Test
    fun addTopicStructures() {
        val mergedTopic = Node.from(MqttUtf8String("sport/+/p1")).root()
        mergedTopic += Node.from(MqttUtf8String("sport/+/p2")).root()
        assertTrue(mergedTopic.matchesTopic(Node.parse("sport/+/p1")))
        assertTrue(mergedTopic.matchesTopic(Node.parse("sport/+/p2")))
    }

    @Test
    fun removeTopicStructures() {
        val mergedTopic = Node.from(MqttUtf8String("sport/+/p1")).root()
        mergedTopic += Node.from(MqttUtf8String("sport/+/p2")).root()

        mergedTopic -= Node.from(MqttUtf8String("sport/+/p2"))
        assertEquals(mergedTopic.children.values.first().children.size, 1)
        assertEquals(mergedTopic.allChildren().first().toString(), "sport/+/p1")
    }

    @Test
    fun multiLevelWildcardNonNormative1() {
        val exampleTopic = Node.parse("sport/tennis/player1/#")
        assertTrue(exampleTopic.matchesTopic(Node.parse("sport/tennis/player1")))
    }

    @Test
    fun multiLevelWildcardNonNormative2() {
        val exampleTopic = Node.parse("sport/tennis/player1/#")
        assertTrue(exampleTopic.matchesTopic(Node.parse("sport/tennis/player1/ranking")))
    }

    @Test
    fun multiLevelWildcardNonNormative3() {
        val exampleTopic = Node.parse("sport/tennis/player1/#")
        assertTrue(exampleTopic.matchesTopic(Node.parse("sport/tennis/player1/score/wimbledon")))
    }

    @Test
    fun multiLevelWildcardNonNormative4ParentLevel() {
        val exampleTopic = Node.parse("sport/#")
        assertNotNull(exampleTopic.find(Name("sport")))
    }

    @Test
    fun multiLevelWildcardNonNormative5EveryMessage() {
        val exampleTopic = Node.parse("#")
        assertTrue(exampleTopic.matchesTopic(Node.parse("sport/tennis/player1/score/wimbledon")))
    }

    @Test
    fun multiLevelWildcardNonNormative6() {
        Node.parse("sport/tennis/#")
    }

    @Test
    fun multiLevelWildcardNonNormative7() {
        try {
            Node.parse("sport/tennis#")
            fail("invalid topic name, should of failed")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun multiLevelWildcardNonNormative8Invalid() {
        val exampleTopic = Node.parse("sport/tennis/player1/#")
        assertNotNull(exampleTopic.find(Name("sport/tennis/player1/score/wimbledon")))
    }

    @Test
    fun singleLevelWildcardNonNormative() {
        val example = Node.parse("sport/tennis/+")
        assertTrue(example.matchesTopic(Node.parse("sport/tennis/player1")))
    }

    @Test
    fun singleLevelWildcardNonNormative1() {
        val example = Node.parse("sport/tennis/+")
        val other = Name("sport/tennis/player2")
        val found = example.find(other)
        assertNotNull(found)
    }

    @Test
    fun singleLevelWildcardNonNormative3Negative() {
        val example = Node.parse("sport/tennis/+")
        assertFalse(example.matchesTopic(Node.parse("sport/tennis/player1/ranking")))
    }

    @Test
    fun singleLevelWildcardMatchesOnlyASingleLevel() {
        val example = Node.parse("sport/+")
        assertTrue(example.matchesTopic(Node.parse("sport/")))
        assertFalse(example.matchesTopic(Node.parse("sport/as/df")))
    }

    @Test
    fun singleLevelWildcardNonNormativeSimplePlusValid() {
        Node.parse("+")
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet2() {
        Node.parse("+/tennis/#")
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet3Negative() {
        try {
            Node.parse("sport+")
            fail("failed to build topic")
        } catch (e: ProtocolError) {
        }
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet4() {
        Node.parse("sport/+/player1")
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet5Match1() {
        val example = Node.parse("+/+")
        assertNotNull(example.find(Name("/finance")))
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet5Match2() {
        val example = Node.parse("/+")
        assertTrue(example.matchesTopic(Node.parse("/finance")))
    }

    @Test
    fun singleLevelWildcardNonNormativeBullet5Match3Negative() {
        val example = Node.parse("+")
        assertFalse(example.matchesTopic(Node.parse("/finance")))
    }
}