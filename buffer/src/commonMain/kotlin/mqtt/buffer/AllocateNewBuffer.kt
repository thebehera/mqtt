package mqtt.buffer

expect fun allocateNewBuffer(size: UInt, limits: BufferMemoryLimit): PlatformBuffer
