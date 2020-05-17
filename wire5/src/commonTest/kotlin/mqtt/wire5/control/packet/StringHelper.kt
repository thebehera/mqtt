@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire5.control.packet

import mqtt.buffer.allocateNewBuffer

fun CharSequence.toCharSequenceBuffer(): CharSequence {
    val buffer = allocateNewBuffer((count() + 2).toUInt(), limits)
    buffer.writeMqttUtf8String(this)
    buffer.resetForRead()
    return buffer.readMqttUtf8StringNotValidated()
}