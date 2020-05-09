@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire

open class MqttException(msg: String, val reasonCode: UByte) : Exception(msg)
open class MalformedPacketException(msg: String) : MqttException(msg, 0x81.toUByte())
open class ProtocolError(msg: String) : MqttException(msg, 0x82.toUByte())
