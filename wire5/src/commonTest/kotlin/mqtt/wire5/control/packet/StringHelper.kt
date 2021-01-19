@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import mqtt.buffer.allocateNewBuffer
import mqtt.wire.buffer.readMqttUtf8StringNotValidated
import mqtt.wire.buffer.writeMqttUtf8String

fun CharSequence.toCharSequenceBuffer(): CharSequence {
    val buffer = allocateNewBuffer((count() + 2).toUInt())
    buffer.writeMqttUtf8String(this)
    buffer.resetForRead()
    return buffer.readMqttUtf8StringNotValidated()
}