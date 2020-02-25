package mqtt.transport


data class IncomingMessage(val bytesRead: Int, val byte1: Byte, val remainingLength: UInt, val buffer: PlatformBuffer)