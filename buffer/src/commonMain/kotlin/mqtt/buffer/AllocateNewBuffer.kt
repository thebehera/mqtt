@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.buffer

expect fun allocateNewBuffer(size: UInt, limits: BufferMemoryLimit = DefaultMemoryLimit): PlatformBuffer
