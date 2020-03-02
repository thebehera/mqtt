package mqtt.buffer

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files

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