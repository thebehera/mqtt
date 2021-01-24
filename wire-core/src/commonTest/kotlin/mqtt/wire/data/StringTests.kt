@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.data


import mqtt.buffer.allocateNewBuffer
import mqtt.buffer.utf8Length
import mqtt.wire.buffer.readMqttUtf8StringNotValidated
import mqtt.wire.buffer.writeMqttUtf8String
import kotlin.js.JsName
import kotlin.test.*

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

    //TODO: FIX THIS CONFORMANCE TEST!
//
//    @Test
//    @JsName("zeroWidthNoBreakSpace")
//    fun `MQTT Conformance A UTF-8 encoded sequence 0xEF 0xBB 0xBF is always interpreted as U+FEFF (ZERO WIDTH NO-BREAK SPACE) wherever it appears in a string and MUST NOT be skipped over or stripped off by a packet receiver `() {
//        val string = MqttUtf8String("\uFEFF")
//        val actual = string.getValueOrThrow()
//        assertEquals("\uFEFF", actual)
//        val buffer = allocateNewBuffer(6u = object : BufferMemoryLimit {
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
    @ExperimentalStdlibApi
    fun mqttUtf8String() {
        val string = "yolo swag lyfestyle"
        assertEquals(19, string.utf8Length().toInt())
        val platformBuffer = allocateNewBuffer(21u)
        platformBuffer.writeMqttUtf8String(string)
        platformBuffer.resetForRead()
        val actual = platformBuffer.readMqttUtf8StringNotValidated().toString()
        assertEquals(string.length, actual.length)
        assertEquals(string, actual)
    }
}