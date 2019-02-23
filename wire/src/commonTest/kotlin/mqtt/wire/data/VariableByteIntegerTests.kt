package mqtt.wire.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class VariableByteIntegerTests {

    @Test
    fun encodeAndDecode() {
        val numberToEncode = 268435442
        val encodedNumber = numberToEncode.encodeVariableByteInteger()
        val decoded = encodedNumber.decodeVariableByteInteger()
        assertEquals(numberToEncode, decoded)
    }

    @Test
    fun oversize() {
        try {
            val encodedByteArray = Int.MAX_VALUE.encodeVariableByteInteger()
            encodedByteArray.decodeVariableByteInteger()
            fail("Should of have hit an exception")
        } catch (e: MqttMalformedInvalidVariableByteInteger) {
        }
    }
}
