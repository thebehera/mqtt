@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import kotlinx.atomicfu.atomic

private val packetIdentifierCount = atomic(0)


fun getAndIncrementPacketIdentifier() = packetIdentifierCount.incrementAndGet().toUShort().toInt()
