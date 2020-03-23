@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package mqtt.wire5.control.packet

import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class PingResponseTests {
    @Test
    fun serializeDeserialize() {
        val buffer = allocateNewBuffer(2u, limits)
        val ping = PingResponse
        ping.serialize(buffer)
        buffer.resetForRead()
        assertEquals(13.shl(4).toByte(), buffer.readByte())
        assertEquals(0, buffer.readByte())
        buffer.resetForRead()
        val result = ControlPacketV5.from(buffer)
        assertEquals(result, ping)
    }
}
