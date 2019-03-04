package mqtt.wire

import kotlinx.io.errors.IOException

open class MqttException(msg: String, val reasonCode: Byte) : IOException(msg)
open class MalformedPacketException(msg: String) : MqttException(msg, 0x81.toByte())


class MalformedInvalidVariableByteInteger(val value: Int) : IOException("Malformed Variable Byte Integer: This " +
        "property must be a number between 0 and %VARIABLE_BYTE_INT_MAX . Read unsigned4BitValue was: $value")