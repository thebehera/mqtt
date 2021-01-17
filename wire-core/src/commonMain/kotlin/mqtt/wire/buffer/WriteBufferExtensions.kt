package mqtt.wire.buffer

import mqtt.buffer.VARIABLE_BYTE_INT_MAX
import mqtt.buffer.WriteBuffer
import mqtt.buffer.utf8Length
import kotlin.experimental.or
import kotlin.reflect.KClass


fun WriteBuffer.writeMqttUtf8String(charSequence: CharSequence): WriteBuffer {
    val string = charSequence.toString()
    val size = string.utf8Length().toUShort()
    write(size)
    writeUtf8(string)
    return this
}

fun WriteBuffer.writeGenericType(genericType: GenericType<*>): WriteBuffer {
    GenericSerialization.serialize(this, genericType)
    return this
}

fun <T : Any> sizeGenericType(obj: T, kClass: KClass<T>) = GenericSerialization.size(obj, kClass)

fun WriteBuffer.writeVariableByteInteger(uInt: UInt): WriteBuffer {
    if (uInt !in 0.toUInt()..VARIABLE_BYTE_INT_MAX.toUInt()) {
        throw MalformedInvalidVariableByteInteger(uInt)
    }
    var numBytes = 0
    var no = uInt.toLong()
    do {
        var digit = (no % 128).toByte()
        no /= 128
        if (no > 0) {
            digit = digit or 0x80.toByte()
        }
        write(digit)
        numBytes++
    } while (no > 0 && numBytes < 4)
    return this
}

fun variableByteIntegerSize(uInt: UInt): UInt {
    if (uInt !in 0.toUInt()..VARIABLE_BYTE_INT_MAX) {
        throw MalformedInvalidVariableByteInteger(uInt)
    }
    var numBytes = 0
    var no = uInt.toLong()
    do {
        (no % 128).toByte()
        no /= 128
        numBytes++
    } while (no > 0 && numBytes < 4)
    return numBytes.toUInt()
}