@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.buffer

import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.encodeToByteArray
import io.ktor.utils.io.core.*
import io.ktor.utils.io.core.internal.DangerousInternalIoApi

@OptIn(DangerousInternalIoApi::class, ExperimentalStdlibApi::class)
data class NativeBuffer constructor(val buffer: Buffer) : PlatformBuffer {
    override val type = BufferType.InMemory

    override fun resetForRead() = buffer.resetForRead()
    override fun resetForWrite() = buffer.resetForWrite()

    override fun readByte() = buffer.readByte()

    override fun readByteArray(size: UInt) = buffer.readBytes(size.toInt())

    override fun readUnsignedByte() = buffer.readUByte()

    override fun readUnsignedShort() = buffer.readUShort()

    override fun readUnsignedInt() = buffer.readUInt()
    override fun readLong() = buffer.readLong()

    override fun readMqttUtf8StringNotValidatedSized(): Pair<UInt, CharSequence> {
        val length = readUnsignedShort()
        val bytes = buffer.readBytes(length.toInt())
        val text = bytes.decodeToString()
        return Pair(length.toUInt(), text)
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

    override fun write(long: Long): WriteBuffer {
        buffer.writeLong(long)
        return this
    }

    override fun writeUtf8String(charSequence: CharSequence): WriteBuffer {
        buffer.writeUShort(mqttUtf8Size(charSequence).toUShort())
        buffer.writeFully(Charsets.UTF_8.newEncoder().encodeToByteArray(charSequence))
        return this
    }

    override fun mqttUtf8Size(
        inputSequence: CharSequence,
        malformedInput: CharSequence?,
        unmappableCharacter: CharSequence?
    ): UInt {
        val size = Charsets.UTF_8.newEncoder().encodeToByteArray(inputSequence).size.toUInt()
        println("size $inputSequence = $size")
        return size
    }

    override fun utf8StringSize(
        inputSequence: CharSequence,
        malformedInput: CharSequence?,
        unmappableCharacter: CharSequence?
    ) = mqttUtf8Size(inputSequence, malformedInput, unmappableCharacter)

    override suspend fun close() {}
}


actual fun allocateNewBuffer(
    size: UInt,
    limits: BufferMemoryLimit
): PlatformBuffer {
    return NativeBuffer(IoBuffer.Pool.borrow())
}