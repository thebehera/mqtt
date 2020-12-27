package mqtt

import mqtt.wire.control.packet.*

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

//    override suspend fun store(pub: IPublishReceived) {
//        val packetIdentifier = pub.packetIdentifier.toUShort()
//        outgoingQueued[packetIdentifier] = pub
//    }

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