package mqtt.buffer

import kotlin.experimental.or

@ExperimentalUnsignedTypes
interface WriteBuffer {
    fun write(byte: Byte): WriteBuffer
    fun write(byte: ByteArray): WriteBuffer
    fun write(uByte: UByte): WriteBuffer
    fun write(uShort: UShort): WriteBuffer
    fun write(uInt: UInt): WriteBuffer
    fun writeUtf8String(charSequence: CharSequence): WriteBuffer
    fun writeVariableByteInteger(uInt: UInt): WriteBuffer {
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

    fun mqttUtf8Size(
        inputSequence: CharSequence,
        malformedInput: CharSequence? = null,
        unmappableCharacter: CharSequence? = null
    ): UInt

    fun variableByteIntegerSize(uInt: UInt): UInt {
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
        return numBytes.toUInt()
    }

//    fun <T> write(serializationStrategy: MqttSerializationStrategy<T>): WriteBuffer
    // mqtt 5
    // fun write(property:Property)
}