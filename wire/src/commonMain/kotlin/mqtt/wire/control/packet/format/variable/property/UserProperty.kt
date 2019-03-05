package mqtt.wire.control.packet.format.variable.property

import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.Type

data class UserProperty(val key: MqttUtf8String, val value: MqttUtf8String) : Property(0x26,
        Type.UTF_8_ENCODED_STRING, willProperties = true)
