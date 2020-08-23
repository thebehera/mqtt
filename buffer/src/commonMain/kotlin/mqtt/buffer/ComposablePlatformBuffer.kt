package mqtt.buffer

@ExperimentalUnsignedTypes
class ComposablePlatformBuffer(
    private val first: ReadBuffer,
    private val second: ReadBuffer,
    private val pool: BufferPool = BufferPool(UnlimitedMemoryLimit)
) : ReadBuffer {
    private val firstInitialLimit = first.limit()
    private val secondInitialLimit = second.limit()
    private var currentPosition = 0u


    override fun limit() = firstInitialLimit + secondInitialLimit

    override fun position() = currentPosition

    override fun position(newPosition: Int) {
        currentPosition = newPosition.toUInt()
    }

    override fun resetForRead() {
        currentPosition = 0u
    }

    override fun readByte(): Byte {
        return if (currentPosition++ < firstInitialLimit) {
            first.readByte()
        } else {
            second.readByte()
        }
    }

    private fun readSizeIntoBuffer(size: UInt): ReadBuffer {
        val buffer = if (currentPosition < firstInitialLimit && currentPosition + size < firstInitialLimit) {
            first
        } else if (currentPosition < firstInitialLimit && currentPosition + size > firstInitialLimit) {
            val firstChunkSize = firstInitialLimit - currentPosition
            val secondChunkSize = size - firstChunkSize
            val buffer = pool.borrowAsync(size)
            buffer.write(first.readByteArray(firstChunkSize))
            buffer.write(second.readByteArray(secondChunkSize))
            buffer.resetForRead()
            buffer
        } else {
            second
        }
        currentPosition += size
        return buffer
    }

    override fun readByteArray(size: UInt): ByteArray {
        return readSizeIntoBuffer(size).readByteArray(size)
    }

    override fun readUnsignedByte(): UByte {
        return if (currentPosition++ < firstInitialLimit) {
            first.readUnsignedByte()
        } else {
            second.readUnsignedByte()
        }
    }

    override fun readUnsignedShort() = readSizeIntoBuffer(UShort.SIZE_BYTES.toUInt()).readUnsignedShort()

    override fun readUnsignedInt() = readSizeIntoBuffer(UInt.SIZE_BYTES.toUInt()).readUnsignedInt()


    override fun readLong() = readSizeIntoBuffer(ULong.SIZE_BYTES.toUInt()).readLong()

    override fun readUtf8(bytes: UInt) = readSizeIntoBuffer(bytes).readUtf8(bytes)

    override fun sizeUtf8String(
        inputSequence: CharSequence,
        malformedInput: CharSequence?,
        unmappableCharacter: CharSequence?
    ) = first.sizeUtf8String(inputSequence, malformedInput, unmappableCharacter)
}

@ExperimentalUnsignedTypes
fun List<PlatformBuffer>.toComposableBuffer(): ReadBuffer {
    return when (size) {
        1 -> {
            first()
        }
        else -> {
            ComposablePlatformBuffer(
                first(),
                subList(1, size).toComposableBuffer()
            )
        }
    }
}