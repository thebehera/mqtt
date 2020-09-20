@file:Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION")

package mqtt.buffer

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.core.internal.*

@OptIn(DangerousInternalIoApi::class, ExperimentalStdlibApi::class)
data class NativeBuffer constructor(val buffer: Buffer) : PlatformBuffer {
    override val type = BufferType.InMemory
    override fun limit() = buffer.limit.toUInt()
    override fun position() = buffer.writePosition.toUInt()

    override fun resetForRead() = buffer.resetForRead()
    override fun resetForWrite() = buffer.resetForWrite()

    override fun readByte() = buffer.readByte()
    override val capacity: UInt = buffer.capacity.toUInt()

    override fun readByteArray(size: UInt) = buffer.readBytes(size.toInt())

    override fun readUnsignedByte() = buffer.readUByte()

    override fun readUnsignedShort() = buffer.readUShort()

    override fun readUnsignedInt() = buffer.readUInt()
    override fun readLong() = buffer.readLong()

    override fun readUtf8(bytes: UInt): CharSequence {
        return buffer.readBytes(bytes.toInt()).decodeToString()
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

    override fun writeUtf8(text: CharSequence): WriteBuffer {
        val bytes = Charsets.UTF_8.newEncoder().encodeToByteArray(text)
        buffer.writeFully(bytes)
        return this
    }

    override fun sizeUtf8String(
        inputSequence: CharSequence,
        malformedInput: CharSequence?,
        unmappableCharacter: CharSequence?
    ) = Charsets.UTF_8.newEncoder().encodeToByteArray(inputSequence).size.toUInt()

    override fun lengthUtf8String(
        inputSequence: CharSequence,
        malformedInput: CharSequence?,
        unmappableCharacter: CharSequence?
    ) = sizeUtf8String(inputSequence, malformedInput, unmappableCharacter)


    override fun write(buffer: PlatformBuffer) {
        this.buffer.writeFully((buffer as NativeBuffer).buffer)
    }

    override fun position(newPosition: Int) {
        this.buffer.rewind(newPosition - this.buffer.readPosition)
    }

    override suspend fun close() {}
}


actual fun allocateNewBuffer(
    size: UInt,
    limits: BufferMemoryLimit
): PlatformBuffer {
//    return NativeBuffer(IoBuffer.Pool.borrow())
    @OptIn(DangerousInternalIoApi::class)
    return NativeBuffer(Buffer(IoBuffer.Pool.borrow().memory.slice(0, size.toInt())))
}

actual fun String.toBuffer(): PlatformBuffer {
    val array = Charsets.UTF_8.newEncoder().encodeToByteArray(this)

    @OptIn(DangerousInternalIoApi::class)
    val buffer = NativeBuffer(Buffer(IoBuffer.Pool.borrow().memory.slice(0, array.size)))
    buffer.write(array)
    buffer.resetForRead()
    return buffer
}