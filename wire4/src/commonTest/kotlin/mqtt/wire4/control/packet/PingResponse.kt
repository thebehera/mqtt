package mqtt.wire4.control.packet


import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class PingResponseTests {
    @Test
    fun serializeDeserialize() {
        val ping = PingResponse
        val buffer = allocateNewBuffer(2u)
        ping.serialize(buffer)
        buffer.resetForRead()
        assertEquals(13.shl(4).toByte(), buffer.readByte())
        assertEquals(0, buffer.readByte())

        val buffer2 = allocateNewBuffer(2u)
        ping.serialize(buffer2)
        buffer2.resetForRead()
        val result = ControlPacketV4.from(buffer2)
        assertEquals(result, ping)
    }
}
