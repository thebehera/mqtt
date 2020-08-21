@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.buffer

expect fun allocateNewBuffer(size: UInt, limits: BufferMemoryLimit = DefaultMemoryLimit): PlatformBuffer

val sizingBuffer = allocateNewBuffer(100u)

fun CharSequence?.utf8Length(): UInt {
    this ?: return 0u
    return sizingBuffer.sizeUtf8String(this)
}

fun CharSequence.toBuffer() :PlatformBuffer {
    val buffer = allocateNewBuffer(utf8Length())
    buffer.writeUtf8(this)
    return buffer
}