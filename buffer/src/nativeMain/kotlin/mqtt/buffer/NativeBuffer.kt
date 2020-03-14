package mqtt.buffer

import kotlinx.io.charsets.Charsets
import kotlinx.io.charsets.encode
import kotlinx.io.core.*

data class NativeBuffer(val buffer: IoBuffer = IoBuffer.Pool.borrow()) : PlatformBuffer {
    override val type = BufferType.InMemory

    override fun readByte() = buffer.readByte()

    override fun readByteArray() = buffer.readBytes()

    override fun readUnsignedByte() = buffer.readUByte()

    override fun readUnsignedShort() = buffer.readUShort()

    override fun readUnsignedInt() = buffer.readUInt()

    override fun readMqttUtf8StringNotValidated(): kotlin.CharSequence {
        val length = readUnsignedShort().toInt()
        return buffer.readText(max = length)
    }

    override fun put(buffer: PlatformBuffer) = this.buffer.writeFully((buffer as NativeBuffer).buffer)

    override fun write(byte: Byte): WriteBuffer {
        buffer.writeByte(byte)
        return this
    }

    override fun write(bytes: ByteArray): WriteBuffer {
        buffer.writeFully(bytes)
        return this
    }

    override fun write(uByte: UByte): WriteBuffer {
        buffer.writeUByte(uByte)
        return this
    }

    override fun write(uShort: UShort): WriteBuffer {
        buffer.writeUShort(uShort)
        return this
    }

    override fun write(uInt: UInt): WriteBuffer {
        buffer.writeUInt(uInt)
        return this
    }

    override fun writeUtf8String(charSequence: CharSequence): WriteBuffer {
        buffer.writeText(charSequence)
        return this
    }

    override fun mqttUtf8Size(
        inputSequence: CharSequence,
        malformedInput: CharSequence?,
        unmappableCharacter: CharSequence?
    ) = Charsets.UTF_8.newEncoder().encode(inputSequence).remaining.toUInt()

    override suspend fun close() {}
}

actual fun allocateNewBuffer(
    size: UInt,
    limits: BufferMemoryLimit
): PlatformBuffer {
    return NativeBuffer()
}