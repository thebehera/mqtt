@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire

import kotlinx.io.errors.IOException

open class MqttException(msg: String, val reasonCode: UByte) : IOException(msg)
open class MalformedPacketException(msg: String) : MqttException(msg, 0x81.toUByte())
open class ProtocolError(msg: String) : MqttException(msg, 0x82.toUByte())

class MalformedInvalidVariableByteInteger(value: UInt) : IOException(
    "Malformed Variable Byte Integer: This " +
            "property must be a number between 0 and %VARIABLE_BYTE_INT_MAX . Read controlPacketValue was: $value"
)

class BrokerRejectedConnection(val reason: String) : Exception(reason)

class MqttPersistenceException(msg: String) : MqttException(msg, 0x83.toUByte())
