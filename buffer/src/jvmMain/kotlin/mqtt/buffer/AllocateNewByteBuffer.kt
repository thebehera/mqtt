package mqtt.buffer

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.FileChannel
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder
import java.nio.charset.CoderResult
import java.nio.file.Files
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

actual fun allocateNewBuffer(
    size: UInt,
    limits: BufferMemoryLimit
): PlatformBuffer = if (limits.isTooLargeForMemory(size)) {
    val file = Files.createTempFile(limits.tmpBufferPrefix, null).toFile()
    val randomAccessFile = RandomAccessFile(file, "rw")
    val fileMappedBuffer = randomAccessFile.channel!!.map(FileChannel.MapMode.READ_WRITE, 0L, size.toLong())
    JvmBuffer(fileMappedBuffer, randomAccessFile)
} else {
    JvmBuffer(ByteBuffer.allocateDirect(size.toInt()))
}


actual fun String.toBuffer(): PlatformBuffer = JvmBuffer(ByteBuffer.wrap(encodeToByteArray()))

actual fun String.utf8Length(): UInt = encodeToByteArray().size.toUInt()
