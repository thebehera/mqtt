@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client.persistence

import kotlinx.coroutines.CoroutineScope
import mqtt.connection.IRemoteHost
import mqtt.wire.control.packet.ControlPacket

interface QueuedObjectCollection : CoroutineScope {
    val connectionId: Int
    // Only used to open the DB with a concrete filename
    // Room: Create a DB with the ClientID+ServerUrl
    suspend fun open(remoteHost: IRemoteHost)

    // Only used for When IPublishReceived -> qos2MessagesRecevedButNotCompletelyAcked
    suspend fun ackMessageIdQueueControlPacket(ackMsgId: Int, key: UShort, value: ControlPacket)

    suspend fun get(messageId: Int? = null): ControlPacket?

    // Only used to ack Qos1 and Qos2 states
    suspend fun remove(key: UShort)
}
