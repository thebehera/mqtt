package mqtt.buffer

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files

actual fun allocateNewBuffer(
    size: UInt
): PlatformBuffer = try {
    JvmBuffer(ByteBuffer.allocateDirect(size.toInt()))
} catch (e: OutOfMemoryError) {
    val file = Files.createTempFile("com.ditcoom.tmp.mqtt.", null).toFile()
    val randomAccessFile = RandomAccessFile(file, "rw")
    val fileMappedBuffer = randomAccessFile.channel!!.map(FileChannel.MapMode.READ_WRITE, 0L, size.toLong())
    JvmBuffer(fileMappedBuffer, randomAccessFile)
}


actual fun String.toUtf8Buffer(): PlatformBuffer = JvmBuffer(ByteBuffer.wrap(encodeToByteArray()))

actual fun String.utf8Length(): UInt = encodeToByteArray().size.toUInt()
