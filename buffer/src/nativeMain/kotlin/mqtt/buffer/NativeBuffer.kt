package mqtt.buffer

@ExperimentalUnsignedTypes
class NativeBuffer(private val data: ByteArray) : PlatformBuffer {
    override val type = BufferType.InMemory
    override val capacity: UInt = data.size.toUInt()
    private var limit = data.size
    private var position = 0

    override fun put(buffer: PlatformBuffer) {
        write(buffer)
    }

    override fun resetForRead() {
        limit = position
        position = 0
    }

    override fun resetForWrite() {
        position = 0
        limit = data.size
    }

    override fun readByte() = data[position++]

    override fun readByteArray(size: UInt): ByteArray {
        val result = data.copyOfRange(position, position + size.toInt())
        position += size.toInt()
        return result
    }

    override fun readUnsignedByte() = data.getUByteAt(position++)

    override fun readUnsignedShort(): UShort {
        val value = data.getUShortAt(position)
        position += UShort.SIZE_BYTES
        return value
    }

    override fun readUnsignedInt(): UInt {
        val value = data.getUIntAt(position)
        position += UInt.SIZE_BYTES
        return value
    }

    override fun readLong(): Long {
        val value = data.getLongAt(position)
        position += Long.SIZE_BYTES
        return value
    }

    override fun readUtf8(bytes: UInt): CharSequence {
        val value = data.decodeToString(position, position + bytes.toInt())
        position += bytes.toInt()
        return value
    }

    override fun sizeUtf8String(
        inputSequence: CharSequence,
        malformedInput: CharSequence?,
        unmappableCharacter: CharSequence?
    ) = inputSequence.toString().encodeToByteArray().size.toUInt()

    override fun write(byte: Byte): WriteBuffer {
        data[position++] = byte
        return this
    }

    override fun write(bytes: ByteArray): WriteBuffer {
        bytes.copyInto(data, position)
        position += bytes.size
        return this
    }

    override fun write(uByte: UByte) = write(uByte.toByte())

    override fun write(uShort: UShort): WriteBuffer {
        val value = uShort.toInt()
        data[position++] = value.toByte()
        data[position++] = value.shr(8).toByte()
        return this
    }

    override fun write(uInt: UInt): WriteBuffer {
        val value = uInt.toInt()
        data[position++] = (value shr 24 and 0xff).toByte()
        data[position++] = (value shr 16 and 0xff).toByte()
        data[position++] = (value shr 8 and 0xff).toByte()
        data[position++] = (value shr 0 and 0xff).toByte()
        return this
    }

    override fun write(long: Long): WriteBuffer {
        data[position++] = (long shr 56 and 0xff).toByte()
        data[position++] = (long shr 48 and 0xff).toByte()
        data[position++] = (long shr 40 and 0xff).toByte()
        data[position++] = (long shr 32 and 0xff).toByte()
        data[position++] = (long shr 24 and 0xff).toByte()
        data[position++] = (long shr 16 and 0xff).toByte()
        data[position++] = (long shr 8 and 0xff).toByte()
        data[position++] = (long shr 0 and 0xff).toByte()
        return this
    }

    override fun write(buffer: PlatformBuffer) {
        write((buffer as NativeBuffer).data)
    }

    override fun writeUtf8(text: CharSequence): WriteBuffer {
        write(text.toString().encodeToByteArray())
        return this
    }

    override fun lengthUtf8String(
        inputSequence: CharSequence,
        malformedInput: CharSequence?,
        unmappableCharacter: CharSequence?
    ) = sizeUtf8String(inputSequence, malformedInput, unmappableCharacter)

    override suspend fun close() = Unit

    override fun limit() = limit.toUInt()
    override fun position() = position.toUInt()
    override fun position(newPosition: Int) {
        position = newPosition
    }
}

actual fun allocateNewBuffer(
    size: UInt,
    limits: BufferMemoryLimit
): PlatformBuffer = NativeBuffer(ByteArray(size.toInt()))

actual fun String.toBuffer(): PlatformBuffer = NativeBuffer(this.encodeToByteArray())