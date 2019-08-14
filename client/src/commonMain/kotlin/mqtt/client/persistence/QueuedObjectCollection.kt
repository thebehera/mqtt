@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import io.ktor.http.Url
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.data.MqttUtf8String

interface QueuedObjectCollection {
    // Only used to open the DB with a concrete filename
    // Room: Create a DB with the ClientID+ServerUrl
    suspend fun open(clientId: MqttUtf8String, server: Url)

    // Dequeue next object that needs to be sent
    suspend fun dequeue(): ControlPacket?

    // Only used to flush queues with the get(key:Ushort) call
    // Return next Id's upto some limit
    suspend fun keys(limit: UShort = 1.toUShort()): Collection<UShort>

    // Only used for When IPublishReceived -> qos2MessagesRecevedButNotCompletelyAcked
    suspend fun put(key: UShort, value: ControlPacket): ControlPacket?

    // Only used to flush queues after checking keys()
    suspend fun get(key: UShort): ControlPacket?

    // Only used to ack Qos1 and Qos2 states
    suspend fun remove(key: UShort): ControlPacket?

    suspend fun clear()
}
