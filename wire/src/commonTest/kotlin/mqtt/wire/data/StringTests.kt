package mqtt.wire.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringTests {
    @Test
    fun invalidMqttString() = assertFalse("abc\u0001def".validateMqttUTF8String())

    @Test
    fun validMqttString() = assertTrue("abcdef".validateMqttUTF8String())
}
