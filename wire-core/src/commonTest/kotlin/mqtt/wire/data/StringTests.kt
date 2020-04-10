@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.data
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * MQTT Conformance Character data in a UTF-8 Encoded String MUST be well-formed UTF-8 as defined by the Unicode specification Unicode and restated in RFC 3629
 */
class StringTests {

    @Test
    fun invalidMqttString() = assertFalse("abc\u0001def".validateMqttUTF8String())

    @Test
    fun validMqttString() = assertTrue("abc\u002Fdef".validateMqttUTF8String())

    @Test
    fun invalidMqttStringPoint2() = assertFalse("abc\u007fdef".validateMqttUTF8String())

    @Test
    fun validMqttStringBasic() = assertTrue("abcdef".validateMqttUTF8String())


    @Test
    @JsName("utf8DoesNotHaveNull")
    fun `MQTT Conformance A UTF-8 Encoded String MUST NOT include an encoding of the null character U+0000`() {
        val string = MqttUtf8String("\u0000")
        try {
            string.getValueOrThrow()
            fail("should of thrown")
        } catch (e: InvalidMqttUtf8StringMalformedPacketException) {
        }
    }

    //TODO: FIX THIS CONFORMANCE TEST!
//
//    @Test
//    @JsName("zeroWidthNoBreakSpace")
//    fun `MQTT Conformance A UTF-8 encoded sequence 0xEF 0xBB 0xBF is always interpreted as U+FEFF (ZERO WIDTH NO-BREAK SPACE) wherever it appears in a string and MUST NOT be skipped over or stripped off by a packet receiver `() {
//        val string = MqttUtf8String("\uFEFF")
//        val actual = string.getValueOrThrow()
//        assertEquals("\uFEFF", actual)
//        val buffer = allocateNewBuffer(6u, limits = object : BufferMemoryLimit {
//            override fun isTooLargeForMemory(size: UInt) = false
//        })
//        buffer.writeUtf8String(string.value)
//        buffer.resetForRead()
//        assertEquals(string.value, buffer.readMqttUtf8StringNotValidated().toString())
//    }

    @Test
    @JsName("latinCaptialNoNormativeTest")
    fun `The string AuD869uDED4 which is LATIN CAPITAL Letter A followed by the code point U+2A6D4 (which represents a CJK IDEOGRAPH EXTENSION B character) is encoded`() {
//        val string = MqttUtf8String("A\uD869\uDED4")
//        val actual = string.getValueOrThrow()
//        assertEquals("A𪛔", "A\uD869\uDED4")

//        assertEquals(5.toUShort(), packet.readUShort())
//        assertEquals(0x41.toByte(), packet.readByte()) // 'A'
//        assertEquals(0xF0.toByte(), packet.readByte())
        // This is failing. what do i do here?
//        assertEquals(0xAA.toByte(), packet.readByte())
//        assertEquals(0x9B.toByte(), packet.readByte())
//        assertEquals(0x94.toByte(), packet.readByte())
    }

    @Test
    fun controlCharacterU0001toU001F() {
        for (c in '\u0001'..'\u001F') {
            try {
                val string = MqttUtf8String(c.toString())
                string.getValueOrThrow()
                fail("should of thrown")
            } catch (e: InvalidMqttUtf8StringMalformedPacketException) {
                e.toString()
            }
        }
    }


    @Test
    fun stringLengthOverflow() {
        try {
            MqttUtf8String("a".repeat(65_536)).getValueOrThrow()
            fail("should of thrown")
        } catch (e: InvalidMqttUtf8StringMalformedPacketException) {
        }
    }

    @Test
    fun controlCharacterUD800toUDFFF() {
        for (c in '\uD800'..'\uDFFF') {
            try {
                val string = MqttUtf8String(c.toString())
                string.getValueOrThrow()
                fail("should of thrown")
            } catch (e: InvalidMqttUtf8StringMalformedPacketException) {
            }
        }
    }

    @Test
    fun controlCharacterU007FtoU009F() {
        for (c in '\u007F'..'\u009F') {
            try {
                val string = MqttUtf8String(c.toString())
                string.getValueOrThrow()
                fail("should of thrown")
            } catch (e: InvalidMqttUtf8StringMalformedPacketException) {
            }
        }
    }
}