@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.persistence

typealias QueuedObjectId = Long

interface IQueuedMessage {
    val queuedObjectId: QueuedObjectId
    val childTableName: String
    val childRowId: Long
    val priority: Double

    // Null until first attempt to deque
    val messageId: Int?
}

data class QueuedMessage(
    override val queuedObjectId: QueuedObjectId,
    override val childTableName: String,
    override val childRowId: Long,
    override val priority: Double = 0.0,
    // Null until first attempt to deque
    override val messageId: Int?
) : IQueuedMessage

data class PeekedQueuedMessage(
    val childTableName: String,
    val childRowId: Long,
    // Null until first attempt to deque
    val messageId: UShort?
)

interface MqttPersistence<T : IQueuedMessage> {
    fun queueNewMessage(tableName: String, rowId: Long, priority: Double): Long
    fun peekQueuedMessages(count: Int = 1): List<T>
    fun attemptCancel(queuedObjectId: QueuedObjectId)
    fun acknowlege(messageId: Int)
}


class InMemoryPersistence : MqttPersistence<QueuedMessage> {
    private val queuedMessages = HashMap<QueuedObjectId, IQueuedMessage>()

    private val priorityComparator = compareByDescending<IQueuedMessage> { it.priority }
    private val messageIdAndPriorityComparator = priorityComparator.thenBy { it.messageId }

    override fun queueNewMessage(tableName: String, rowId: Long, priority: Double): Long {
        val largestMessageId =
            queuedMessages.values.filter { it.messageId != null }.maxByOrNull { it.messageId!! }?.messageId ?: 0
        val nextMessageId = largestMessageId + 1

        val queuedMessage = QueuedMessage(nextMessageId.toLong(), tableName, rowId, priority, nextMessageId)
        queuedMessages[queuedMessage.queuedObjectId] = queuedMessage
        notifyQueuedChange()
        return nextMessageId.toLong()
    }

    override fun peekQueuedMessages(count: Int): List<QueuedMessage> {
        val allQueuedMessages = queuedMessages.values.sortedWith(messageIdAndPriorityComparator)
        val subList = allQueuedMessages.subList(count, allQueuedMessages.size)
        val list = ArrayList<QueuedMessage>(subList.size)
        for (item in subList) {
            list += QueuedMessage(
                item.queuedObjectId,
                item.childTableName,
                item.childRowId,
                item.priority,
                item.messageId
            )
        }
        return list

    }

    override fun attemptCancel(queuedObjectId: QueuedObjectId) {
        if (queuedMessages.remove(queuedObjectId) != null) {
            notifyQueuedChange()
        }
    }

    override fun acknowlege(messageId: Int) {
        val messagesToDelete = queuedMessages.values.filter { it.messageId == messageId }
        messagesToDelete.forEach { queuedMessages.remove(it.queuedObjectId) }
        if (messagesToDelete.isNotEmpty()) {
            notifyQueuedChange()
        }
    }

    fun notifyQueuedChange() {
        // Do some kind of notification here
    }
}
