package mqtt.client.persistence

//@Entity(primaryKeys = ["connectionIdentifier", "topicFilter"])
//@TypeConverters(MqttV4TypeConverters::class)
data class MqttSubscription(
    val connectionIdentifier: Int,
    val topicFilter: String,
    val kclass: String,
    val packetIdentifier: Int = 0
)
