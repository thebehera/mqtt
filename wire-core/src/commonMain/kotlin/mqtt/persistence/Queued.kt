@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.persistence

interface IQueuedMessage {
    val queuedObjectId: Long
    val tableName: String
    val rowId: Long
    // Null until first attempt to deque
    val messageId: UShort?
    val priority: Double
}

data class QueuedMessage(
    override val queuedObjectId: Long,
    override val tableName: String,
    override val rowId: Long,
    // Null until first attempt to deque
    override val messageId: UShort?,
    override val priority: Double = 0.0
) : IQueuedMessage

data class PeekedQueuedMessage(
    val tableName: String,
    val rowId: Long,
    // Null until first attempt to deque
    val messageId: UShort?
)


interface Persistence {
    fun createObjectId(tableName: String, rowId: Long, priority: Double = 0.0): Long
    fun peekQueuedMessages(count: Int = 1, hasMessageIds: Boolean = false): List<PeekedQueuedMessage>
    fun deleteQueued(messageId: UShort)
}
