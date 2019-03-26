package mqtt.wire4.control.packet

import kotlinx.io.core.readBytes
import kotlin.test.Test
import kotlin.test.assertEquals

class PingRequestTests {
    @Test
    fun serializeDeserialize() {
        val ping = PingRequest
        val data = ping.serialize().readBytes()
        assertEquals(data.size, 2)
        assertEquals(data.first(), 12.shl(4).toByte())
        assertEquals(data[1], 0)
        val result = ControlPacketV4.from(ping.serialize())
        assertEquals(result, ping)
    }
}
