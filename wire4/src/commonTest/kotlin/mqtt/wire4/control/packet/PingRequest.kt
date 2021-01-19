package mqtt.wire4.control.packet


import mqtt.buffer.allocateNewBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class PingRequestTests {
    @Test
    fun serializeDeserialize() {
        val ping = PingRequest
        val buffer = allocateNewBuffer(4u)
        ping.serialize(buffer)
        buffer.resetForRead()
        assertEquals(12.shl(4).toByte(), buffer.readByte())
        assertEquals(0, buffer.readByte())

        val buffer2 = allocateNewBuffer(4u)
        ping.serialize(buffer2)
        buffer2.resetForRead()
        val result = ControlPacketV4.from(buffer2)
        assertEquals(result, ping)
    }
}
