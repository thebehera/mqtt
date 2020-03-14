package mqtt.buffer

import kotlinx.io.core.IoBuffer


data class NativeBuffer(val buffer: IoBuffer = IoBuffer.Pool.borrow()) : PlatformBuffer {
    override val type = BufferType.InMemory
    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun limit(newLimit: Int) {
        TODO("Not yet implemented")
    }

    override fun readByte(): kotlin.Byte {
        TODO("Not yet implemented")
    }

    override fun readByteArray(): kotlin.ByteArray {
        TODO("Not yet implemented")
    }

    override fun readUnsignedByte(): kotlin.UByte {
        TODO("Not yet implemented")
    }

    override fun readUnsignedShort(): kotlin.UShort {
        TODO("Not yet implemented")
    }

    override fun readUnsignedInt(): kotlin.UInt {
        TODO("Not yet implemented")
    }

    override fun readMqttUtf8StringNotValidated(): kotlin.CharSequence {
        TODO("Not yet implemented")
    }

    override fun position(): kotlin.Int {
        TODO("Not yet implemented")
    }

    override fun limit(): kotlin.Int {
        TODO("Not yet implemented")
    }

    override fun put(buffer: PlatformBuffer) {
        TODO("Not yet implemented")
    }

    override fun flip() {
        TODO("Not yet implemented")
    }

    override fun setPosition(position: kotlin.Int) {
        TODO("Not yet implemented")
    }

    override fun remaining(): Int {
        TODO("Not yet implemented")
    }

    override fun write(byte: kotlin.Byte): mqtt.buffer.WriteBuffer {
        TODO("Not yet implemented")
    }

    override fun write(byte: ByteArray): WriteBuffer {
        TODO("Not yet implemented")
    }

    override fun write(uByte: UByte): WriteBuffer {
        TODO("Not yet implemented")
    }

    override fun write(uShort: UShort): WriteBuffer {
        TODO("Not yet implemented")
    }

    override fun write(uInt: UInt): WriteBuffer {
        TODO("Not yet implemented")
    }

    override fun writeUtf8String(charSequence: CharSequence): WriteBuffer {
        TODO("Not yet implemented")
    }

    override fun mqttUtf8Size(
        inputSequence: CharSequence,
        malformedInput: CharSequence?,
        unmappableCharacter: CharSequence?
    ): UInt {
        TODO("Not yet implemented")
    }

    override suspend fun close() {
        TODO("Not yet implemented")
    }


}

actual fun allocateNewBuffer(
    size: UInt,
    limits: BufferMemoryLimit
): PlatformBuffer {
    return NativeBuffer()
}