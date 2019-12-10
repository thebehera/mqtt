package mqtt.client.session.transport

import kotlinx.io.core.ByteReadPacket
import mqtt.wire.control.packet.ControlPacket

interface Transport {
    suspend fun writePacket(packet: ByteReadPacket)
    suspend fun read(): ControlPacket
    fun dispose()
    suspend fun awaitClosed()
    val isClosed: Boolean
    val isWebSocket: Boolean
}
