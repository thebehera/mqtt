package mqtt.client.persistence

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mqtt.persistence.db.*
import mqtt.wire.buffer.GenericType
import mqtt.wire.control.packet.*
import mqtt.wire.data.QualityOfService
import mqtt.wire.data.topic.Filter
import mqtt.wire4.control.packet.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface Persistence {
    suspend fun acknowledge(incomingControlPacket: ISubscribeAcknowledgement)
    suspend fun acknowledge(incomingControlPacket: IUnsubscribeAckowledgment)

    suspend fun storeOutgoing(pub: IPublishMessage)

    /**
     * Store just the packet identifier from the publish message.
     * @return true if the packetIdentifier was newly added, false if the packet identifier was already found.
     */
    suspend fun storeIncoming(packetIdentifier: UShort): Boolean

    /** Discard the stored Publish message that was previously sent **/
    suspend fun ack(puback: IPublishAcknowledgment)
    suspend fun release(pubcomp: IPublishComplete)
    suspend fun complete(pubcomp: IPublishComplete)
    suspend fun subscribe(sub: ISubscribeRequest)
    suspend fun unsubscribe(unsub: IUnsubscribeRequest)

    suspend fun queuedMessages(): Map<UShort, ControlPacket>
    suspend fun received(incomingControlPacket: IPublishRelease)

    suspend fun leasePacketIdentifier(factory: ControlPacketFactory): UShort

    suspend fun clear()
}

class DatabasePersistence(
    val database:Database,
    private val dispatcher: CoroutineDispatcher,
    private val connectionId: Long
) : Persistence {


    private val queries4 = database.controlPacketMqtt4Queries
    override suspend fun acknowledge(incomingControlPacket: ISubscribeAcknowledgement) = withContext(dispatcher) {
        queries4.onSubscribeAck4(connectionId, incomingControlPacket.packetIdentifier.toLong())
    }

    override suspend fun acknowledge(incomingControlPacket: IUnsubscribeAckowledgment) = withContext(dispatcher) {
        queries4.onUnsubscribeAck4(connectionId, incomingControlPacket.packetIdentifier.toLong())
    }

    override suspend fun storeOutgoing(pub: IPublishMessage) {
        if (pub !is PublishMessage<*>) return
        withContext(dispatcher) {
            database.transaction {
                val unusedPacketId = queries4.findUnusedPacketIdentifier(connectionId).executeAsOne()
                queries4.publish4(
                    connectionId,
                    unusedPacketId,
                    if (pub.fixed.dup) 1 else 0,
                    pub.fixed.qos.integerValue.toLong(),
                    if (pub.fixed.retain) 1 else 0,
                    pub.variable.topicName.toString(),
                    null
                )
            }
        }
    }

    suspend fun suspendUntilConnectionRemoval() = withContext(dispatcher) {
        val query = database.connectionsQueries.findConnectionId(connectionId)
        suspendCoroutine<Unit> {
            query.addListener(object : Query.Listener {
                override fun queryResultsChanged() {
                    val listener = this
                    launch(Dispatchers.Default) {
                        val result = withContext(dispatcher) {query.executeAsOneOrNull()}
                        if (result == null) {
                            query.removeListener(listener)
                            it.resume(Unit)
                        }
                    }
                }
            })
        }

    }

    override suspend fun storeIncoming(packetIdentifier: UShort) = withContext(dispatcher) {
        database.transactionWithResult<Boolean> {
            queries4.incomingPubRecAboutToSendPubRel4(connectionId, packetIdentifier.toLong())
            queries4.numberOfAffectedRows().executeAsOne() > 0
        }
    }

    override suspend fun ack(puback: IPublishAcknowledgment) = withContext(dispatcher) {
        queries4.incomingPublishAcknowlege4(connectionId, puback.packetIdentifier.toLong())
    }

    override suspend fun release(pubcomp: IPublishComplete) = withContext(dispatcher) {
        queries4.incomingPubRelAboutToSendPubComp(connectionId, pubcomp.packetIdentifier.toLong())
    }

    override suspend fun complete(pubcomp: IPublishComplete) = withContext(dispatcher) {
        queries4.incomingPubcomp(connectionId, pubcomp.packetIdentifier.toLong())
    }

    override suspend fun subscribe(sub: ISubscribeRequest) = withContext(dispatcher) {
        if (sub !is SubscribeRequest) return@withContext
        database.transaction {
            val unusedPacketId = queries4.findUnusedPacketIdentifier(connectionId).executeAsOne()
            sub.subscriptions.forEach {
                queries4.subscription4(
                    connectionId,
                    unusedPacketId,
                    it.topicFilter.topicFilter.toString(),
                    it.maximumQos.integerValue.toLong()
                )
            }
        }
    }

    override suspend fun unsubscribe(unsub: IUnsubscribeRequest) = withContext(dispatcher) {
        if (unsub !is UnsubscribeRequest) return@withContext
        database.transaction {
            val unusedPacketId = queries4.findUnusedPacketIdentifier(connectionId).executeAsOne()
            unsub.topics.forEach {
                queries4.unsubscribe4(connectionId, unusedPacketId, it.toString())
            }
        }
    }

    override suspend fun queuedMessages(): Map<UShort, ControlPacket> {
        val map = LinkedHashMap<UShort, ControlPacket>()
        val subs = mutableListOf<SubscriptionRequest4>()
        val unsubs = mutableListOf<UnsubscriptionRequest4>()
        val pubs = mutableListOf<PublishMessage4>()
        val pubRels = mutableListOf<QueuedPubRel4>()
        withContext(dispatcher) {
            database.transaction {
                subs += queries4.findSubscriptions4(connectionId).executeAsList()
                unsubs += queries4.findUnsubscriptions4(connectionId).executeAsList()
                pubs += queries4.findPublish4(connectionId).executeAsList()
                pubRels += queries4.findPubRel4(connectionId).executeAsList()
            }
        }
        subs.forEach {
            val packetId = it.packetIdentifier.toUShort()
            val sub = Subscription(Filter(it.topicFilter), QualityOfService.from(it.maximumQos))
            val subRequest =
                map.getOrPut(packetId) { SubscribeRequest(packetId.toInt(), mutableListOf()) } as SubscribeRequest
            (subRequest.subscriptions as MutableList<Subscription>) += sub
        }
        unsubs.forEach {
            val packetId = it.packetIdentifier.toUShort()
            val unsub =
                map.getOrPut(packetId) { UnsubscribeRequest(packetId.toInt(), mutableListOf()) } as UnsubscribeRequest
            (unsub.topics as MutableList<CharSequence>) += it.topicFilter
        }
        pubs.forEach {
            val packetId = it.packetIdentifier.toUShort()
            val array = it.payload
            map[packetId] = PublishMessage(
                PublishMessage.FixedHeader(it.dup == 1L, QualityOfService.from(it.qos), it.retain == 1L),
                PublishMessage.VariableHeader(it.topicName, packetId.toInt()),
                if (array != null) GenericType(array, ByteArray::class) else null
            )
        }
        pubRels.forEach {
            map[it.packetIdentifier.toUShort()] = PublishRelease(it.packetIdentifier.toInt())
        }
        return map
    }

    override suspend fun received(incomingControlPacket: IPublishRelease) = withContext(dispatcher) {
        queries4.incomingPubRelAboutToSendPubComp(connectionId, incomingControlPacket.packetIdentifier.toLong())
    }

    override suspend fun leasePacketIdentifier(factory: ControlPacketFactory) =
        queries4.findUnusedPacketIdentifier(connectionId)
            .asFlow().mapToOne().first().toUShort()

    override suspend fun clear() {
        database.connectionsQueries.removeConnection(connectionId)
    }

}

class InMemoryPersistence : Persistence {
    private val outgoingQueued = mutableMapOf<UShort, ControlPacket>()
    private val qos2InUseRecvPackets = hashSetOf<UShort>()

    override suspend fun clear() {
        outgoingQueued.clear()
        qos2InUseRecvPackets.clear()
    }

    override suspend fun queuedMessages(): Map<UShort, ControlPacket> {
        return HashMap(outgoingQueued)
    }

    override suspend fun leasePacketIdentifier(factory: ControlPacketFactory): UShort {
        val largestPacketIdentifier = outgoingQueued.keys.lastOrNull() ?: 0.toUShort()
        val nextPacketIdentifier = (largestPacketIdentifier + 1u).toUShort()
        outgoingQueued[nextPacketIdentifier] = factory.reserved()
        return nextPacketIdentifier
    }

    override suspend fun received(incomingControlPacket: IPublishRelease) {
        val packetIdentifier = incomingControlPacket.packetIdentifier.toUShort()
        outgoingQueued[packetIdentifier] = incomingControlPacket
    }

    override suspend fun release(pubcomp: IPublishComplete) {
        outgoingQueued[pubcomp.packetIdentifier.toUShort()] = pubcomp
    }

    override suspend fun ack(puback: IPublishAcknowledgment) {
        outgoingQueued.remove(puback.packetIdentifier)
    }

    override suspend fun acknowledge(incomingControlPacket: ISubscribeAcknowledgement) {
        outgoingQueued.remove(incomingControlPacket.packetIdentifier.toUShort())
    }

    override suspend fun acknowledge(incomingControlPacket: IUnsubscribeAckowledgment) {
        outgoingQueued.remove(incomingControlPacket.packetIdentifier.toUShort())
    }

    override suspend fun storeOutgoing(pub: IPublishMessage) {
        val packetIdentifier = pub.packetIdentifier!!.toUShort()
        outgoingQueued[packetIdentifier] = pub
    }

    override suspend fun storeIncoming(packetIdentifier: UShort): Boolean {
        return if (qos2InUseRecvPackets.contains(packetIdentifier)) {
            false
        } else {
            qos2InUseRecvPackets += packetIdentifier
            true
        }
    }

    override suspend fun complete(pubcomp: IPublishComplete) {
        outgoingQueued.remove(pubcomp.packetIdentifier.toUShort())
    }

    override suspend fun subscribe(sub: ISubscribeRequest) {
        outgoingQueued[sub.packetIdentifier.toUShort()] = sub
    }

    override suspend fun unsubscribe(unsub: IUnsubscribeRequest) {
        outgoingQueued[unsub.packetIdentifier.toUShort()] = unsub
    }

}
