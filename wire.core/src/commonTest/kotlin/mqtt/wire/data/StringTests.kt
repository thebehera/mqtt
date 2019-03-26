@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.data

import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlin.test.*

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
    fun nullTest() {
        val string = MqttUtf8String("\u0000")
        try {
            string.getValueOrThrow()
            fail("should of thrown")
        } catch (e: InvalidMqttUtf8StringMalformedPacketException) {
        }
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
