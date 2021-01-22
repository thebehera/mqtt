@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.buffer


fun allocateNewBuffer(size: Int) = allocateNewBuffer(size.toUInt())

expect fun allocateNewBuffer(size: UInt): PlatformBuffer

expect fun String.toUtf8Buffer(): PlatformBuffer
expect fun String.utf8Length(): UInt
