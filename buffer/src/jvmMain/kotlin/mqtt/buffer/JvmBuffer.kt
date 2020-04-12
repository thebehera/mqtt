package mqtt.buffer

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.MappedByteBuffer
import java.nio.charset.CharsetEncoder
import java.nio.charset.CodingErrorAction
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@ExperimentalUnsignedTypes
data class JvmBuffer(val byteBuffer: ByteBuffer, val fileRef: RandomAccessFile? = null) : PlatformBuffer {

    override val type: BufferType = if (byteBuffer is MappedByteBuffer) {
        BufferType.Disk
    } else {
        BufferType.InMemory
    }

    override fun resetForRead() {
        byteBuffer.flip()
    }

    override fun resetForWrite() {
        byteBuffer.clear()
    }

    override fun readByte() = byteBuffer.get()
    override fun readByteArray(size: UInt) = byteBuffer.toArray(size)

    override fun readUnsignedByte() = readByte().toUByte()

    override fun readUnsignedShort() = byteBuffer.short.toUShort()

    override fun readUnsignedInt() = byteBuffer.int.toUInt()
    override fun readLong() = byteBuffer.long

    override fun readMqttUtf8StringNotValidatedSized(): Pair<UInt, CharSequence> {
        val length = readUnsignedShort().toInt()
        val finalPosition = byteBuffer.position() + length
        val readBuffer = byteBuffer.asReadOnlyBuffer()
        readBuffer.limit(finalPosition)
        val decoded = Charsets.UTF_8.decode(readBuffer)
        byteBuffer.position(finalPosition)
        return Pair(length.toUInt(), decoded)
    }

    override fun put(buffer: PlatformBuffer) {
        byteBuffer.put((buffer as JvmBuffer).byteBuffer)
    }

    override fun write(byte: Byte): WriteBuffer {
        byteBuffer.put(byte)
        return this
    }

    override fun write(byte: ByteArray): WriteBuffer {
        byteBuffer.put(byte)
        return this
    }

    override fun write(uByte: UByte): WriteBuffer {
        byteBuffer.put(uByte.toByte())
        return this
    }

    override fun write(uShort: UShort): WriteBuffer {
        byteBuffer.putShort(uShort.toShort())
        return this
    }

    override fun write(uInt: UInt): WriteBuffer {
        byteBuffer.putInt(uInt.toInt())
        return this
    }

    override fun write(long: Long): WriteBuffer {
        byteBuffer.putLong(long)
        return this
    }

    override fun utf8StringSize(
        inputSequence: CharSequence,
        malformedInput: CharSequence?,
        unmappableCharacter: CharSequence?
    ) = mqttUtf8Size(inputSequence, malformedInput, unmappableCharacter)

    override fun writeUtf8String(charSequence: CharSequence): WriteBuffer {
        val buffer = CharBuffer.wrap(charSequence)
        val size = mqttUtf8Size(charSequence).toUShort()
        write(size)
        val encoder = Charsets.UTF_8.newEncoder()
        encoder.encode(buffer, byteBuffer, true)
        encoder.flush(byteBuffer)
        return this
    }

    override fun mqttUtf8Size(
        inputSequence: CharSequence,
        malformedInput: CharSequence?,
        unmappableCharacter: CharSequence?
    ): UInt {
        val encoder = Charsets.UTF_8.newEncoder()
        encoder.onMalformedInput(codingErrorAction(encoder, malformedInput))
            .onUnmappableCharacter(codingErrorAction(encoder, unmappableCharacter))
        val input = CharBuffer.wrap(inputSequence)
        val result = encoder.encode(input)
        return result.limit().toUInt()
    }


    private fun codingErrorAction(encoder: CharsetEncoder, inputSequence: CharSequence?): CodingErrorAction {
        return if (inputSequence != null && inputSequence.isNotEmpty() && encoder.canEncode(inputSequence)) {
            val encodedReplacement = encoder.encode(CharBuffer.wrap(inputSequence))
            encoder.replaceWith(encodedReplacement.toArray())
            CodingErrorAction.REPLACE
        } else if (inputSequence?.isEmpty() == true) {
            CodingErrorAction.IGNORE
        } else {
            CodingErrorAction.REPORT
        }
    }

    override fun toString() = byteBuffer.toString()

    override suspend fun close() {
        fileRef?.aClose()
    }
}


suspend fun RandomAccessFile.aClose() = suspendCoroutine<Unit> {
    try {
        close()
        it.resume(Unit)
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }
}


fun ByteBuffer.toArray(size: UInt = remaining().toUInt()): ByteArray {
    return if (hasArray()) {
        val result = ByteArray(size.toInt())
        System.arraycopy(this.array(), position(), result, 0, size.toInt())
        result
    } else {
        val byteArray = ByteArray(size.toInt())
        get(byteArray)
        byteArray
    }
}