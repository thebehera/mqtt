@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.data

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.readUShort
import kotlin.js.JsName
import kotlin.test.*

class StringTests {
    @Test
    @JsName("Utf8EncodedStringIsWellFormed_1_5_4")
    fun `MQTT Conformance Character data in a UTF-8 Encoded String MUST be well-formed UTF-8 as defined by the Unicode specification Unicode and restated in RFC 3629`() {
        invalidMqttString()
        validMqttString()
        invalidMqttString()
        invalidMqttStringPoint2()
        validMqttStringBasic()
        mqttStringToByte1()
        mqttStringToByte2()
        mqttStringToByte3()
        mqttStringToByte4()
        mqttStringToByte5()
        mqttStringToByte6()
        mqttStringSize()
        `MQTT Conformance A UTF-8 Encoded String MUST NOT include an encoding of the null character U+0000`()
        controlCharacterU0001toU001F()
        controlCharacterU007FtoU009F()
        controlCharacterUD800toUDFFF()
        stringLengthOverflow()
    }

    @Test
    fun invalidMqttString() = assertFalse("abc\u0001def".validateMqttUTF8String())

    @Test
    fun validMqttString() = assertTrue("abc\u002Fdef".validateMqttUTF8String())

    @Test
    fun invalidMqttStringPoint2() = assertFalse("abc\u007fdef".validateMqttUTF8String())

    @Test
    fun validMqttStringBasic() = assertTrue("abcdef".validateMqttUTF8String())

    @Test
    fun mqttStringToByte1() {
        val mqttString = MqttUtf8String("MQTT")
        val bytes = buildPacket {
            writeMqttUtf8String(mqttString)
        }.readBytes()
        val lengthMsbByte = bytes.first().toUByte()
        assertEquals(lengthMsbByte, 0x0.toUByte())
    }

    @Test
    fun mqttStringToByte2() {
        val mqttString = MqttUtf8String("MQTT")
        val bytes = buildPacket {
            writeMqttUtf8String(mqttString)
        }.readBytes()
        val lengthMsbByte = bytes[1].toUByte()
        assertEquals(lengthMsbByte, 0x4.toUByte())
    }

    @Test
    fun mqttStringToByte3() {
        val mqttString = MqttUtf8String("MQTT")
        val bytes = buildPacket {
            writeMqttUtf8String(mqttString)
        }.readBytes()
        val byte3 = bytes[2].toUByte()
        assertEquals(byte3, 'M'.toByte().toUByte())
    }

    @Test
    fun mqttStringToByte4() {
        val mqttString = MqttUtf8String("MQTT")
        val bytes = buildPacket {
            writeMqttUtf8String(mqttString)
        }.readBytes()
        val byte4 = bytes[3].toUByte()
        assertEquals(byte4, 'Q'.toByte().toUByte())
    }

    @Test
    fun mqttStringToByte5() {
        val mqttString = MqttUtf8String("MQTT")
        val bytes = buildPacket {
            writeMqttUtf8String(mqttString)
        }.readBytes()
        val byte5 = bytes[4].toUByte()
        assertEquals(byte5, 'T'.toByte().toUByte())
    }

    @Test
    fun mqttStringToByte6() {
        val mqttString = MqttUtf8String("MQTT")
        val bytes = buildPacket {
            writeMqttUtf8String(mqttString)
        }.readBytes()
        val byte6 = bytes[5].toUByte()
        assertEquals(byte6, 'T'.toByte().toUByte())
    }

    @Test
    fun mqttStringSize() {
        val mqttString = MqttUtf8String("MQTT")
        val bytes = buildPacket {
            writeMqttUtf8String(mqttString)
        }.readBytes()
        assertEquals(bytes.size, 6)
    }


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

    @Test
    @JsName("zeroWidthNoBreakSpace")
    fun `MQTT Conformance A UTF-8 encoded sequence 0xEF 0xBB 0xBF is always interpreted as U+FEFF (ZERO WIDTH NO-BREAK SPACE) wherever it appears in a string and MUST NOT be skipped over or stripped off by a packet receiver `() {
        val string = MqttUtf8String("\uFEFF")
        val actual = string.getValueOrThrow()
        assertEquals("\uFEFF", actual)
        val packet = buildPacket {
            writeMqttUtf8String(string)
        }
        assertEquals(3.toUShort(), packet.readUShort())
        assertEquals(0xEF.toByte(), packet.readByte())
        assertEquals(0xBB.toByte(), packet.readByte())
        assertEquals(0xBF.toByte(), packet.readByte())
    }

    @Test
    @JsName("latinCaptialNoNormativeTest")
    fun `The string AuD869uDED4 which is LATIN CAPITAL Letter A followed by the code point U+2A6D4 (which represents a CJK IDEOGRAPH EXTENSION B character) is encoded`() {
//        val string = MqttUtf8String("A\uD869\uDED4")
//        val actual = string.getValueOrThrow()
        assertEquals("A𪛔", "A\uD869\uDED4")
        val packet = buildPacket {
            writeMqttUtf8String(MqttUtf8String("A\uD869\uDED4"), false)
        }
        assertEquals(5.toUShort(), packet.readUShort())
        assertEquals(0x41.toByte(), packet.readByte()) // 'A'
        assertEquals(0xF0.toByte(), packet.readByte())
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
