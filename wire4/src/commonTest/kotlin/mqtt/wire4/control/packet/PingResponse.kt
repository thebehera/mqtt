package mqtt.wire4.control.packet

import kotlinx.io.core.readBytes
import kotlin.test.Test
import kotlin.test.assertEquals

class PingResponseTests {
    @Test
    fun serializeDeserialize() {
        val ping = PingResponse
        val data = ping.serialize().readBytes()
        assertEquals(data.size, 2)
        assertEquals(data.first(), 13.shl(4).toByte())
        assertEquals(data[1], 0)
        val result = ControlPacket.from(ping.serialize())
        assertEquals(result, ping)
    }
}
