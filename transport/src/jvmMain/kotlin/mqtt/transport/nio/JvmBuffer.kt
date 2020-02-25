package mqtt.transport.nio

import mqtt.transport.BufferType
import mqtt.transport.PlatformBuffer
import mqtt.wire.data.WriteBuffer
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
    val encoder = Charsets.UTF_8.newEncoder()

    override val type: BufferType = if (byteBuffer is MappedByteBuffer) {
        BufferType.Disk
    } else {
        BufferType.InMemory
    }

    override fun clear() {
        byteBuffer.clear()
    }

    override fun readByte() = byteBuffer.get()
    override fun readByteArray() = byteBuffer.toArray()

    override fun readUnsignedByte() = readByte().toUByte()

    override fun readUnsignedShort() = byteBuffer.short.toUShort()

    override fun readUnsignedInt() = byteBuffer.int.toUInt()

    override fun readMqttUtf8StringNotValidated(): CharSequence {
        val length = readUnsignedShort().toInt()
        val oldLimit = byteBuffer.limit()
        val newLimit = byteBuffer.position() + length
        byteBuffer.limit(newLimit)
        val decoded = Charsets.UTF_8.decode(byteBuffer)
        byteBuffer.limit(oldLimit + length)
        return decoded
    }

    override fun position() = byteBuffer.position()
    override fun limit() = byteBuffer.limit()
    override fun limit(newLimit: Int) {
        byteBuffer.limit(newLimit)
    }

    override fun put(buffer: PlatformBuffer) {
        byteBuffer.put((buffer as JvmBuffer).byteBuffer)
    }

    override fun remaining() = byteBuffer.remaining()
    override fun setPosition(position: Int) {
        byteBuffer.position(position)
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

    override fun writeUtf8String(charSequence: CharSequence): WriteBuffer {
        val buffer = CharBuffer.wrap(charSequence)
        write(buffer.length.toUShort())
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
        val output = ByteBuffer.allocate(10)
        val limit = input.limit()
        var totalEncoded = Short.SIZE_BYTES
        while (input.position() < limit) {
            output.clear()
            input.mark()
            input.limit((input.position() + 2).coerceAtLeast(input.capacity()))
            input.limit(input.position())
            input.reset()
            encoder.encode(input, output, false)
            totalEncoded += output.position()
        }
        return totalEncoded.toUInt()
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


fun ByteBuffer.toArray(): ByteArray {
    return if (hasArray()) {
        this.array()
    } else {
        val byteArray = ByteArray(remaining())
        get(byteArray)
        byteArray
    }
}