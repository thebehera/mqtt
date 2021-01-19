@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.buffer

interface PlatformBuffer : ReadBuffer, WriteBuffer, SuspendCloseable {
    val capacity: UInt
    fun put(buffer: PlatformBuffer)
}

