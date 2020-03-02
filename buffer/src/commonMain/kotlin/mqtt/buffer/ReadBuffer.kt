package mqtt.buffer

import kotlin.experimental.and

@ExperimentalUnsignedTypes
interface ReadBuffer {
    fun readByte(): Byte
    fun readByteArray(): ByteArray
    fun readUnsignedByte(): UByte
    fun readUnsignedShort(): UShort
    fun readUnsignedInt(): UInt
    fun readMqttUtf8StringNotValidated(): CharSequence

    fun position(): Int
    fun limit(): Int
    fun setPosition(position: Int)
    fun remaining(): Int

    fun readVariableByteInteger(): UInt {
        var digit: Byte
        var value = 0.toUInt()
        var multiplier = 1.toUInt()
        var count = 0.toUInt()
        try {
            do {
                digit = readByte()
                count++
                value += (digit and 0x7F).toUInt() * multiplier
                multiplier *= 128.toUInt()
            } while ((digit and 0x80.toByte()).toInt() != 0)
        } catch (e: Exception) {
            throw MalformedInvalidVariableByteInteger(value)
        }
        if (value < 0.toUInt() || value > VARIABLE_BYTE_INT_MAX.toUInt()) {
            throw MalformedInvalidVariableByteInteger(value)
        }
        return value
    }

//    fun <T> readTyped(deserializationStrategy: MqttDeserializationStrategy<T>): T
    // mqtt 5
    // fun readProperty():Property
}