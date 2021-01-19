package mqtt.client.websocket

import kotlinx.coroutines.channels.ClosedReceiveChannelException
import mqtt.buffer.allocateNewBuffer
import mqtt.client.socket.readVariableByteInteger
import mqtt.socket.SuspendingInputStream
import mqtt.wire.buffer.variableByteSize
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketFactory
import kotlin.experimental.and
import kotlin.math.min
import kotlin.time.ExperimentalTime

@ExperimentalUnsignedTypes
@ExperimentalTime
class WebsocketSuspendableInputStream(
    val inputStream: SuspendingInputStream,
    val controlPacketFactory: ControlPacketFactory
) {
    var currentFrame: FrameMetadata? = null

    data class FrameMetadata(
        val masked: Boolean,
        val payloadLength: Int
    )

    private suspend fun readFrameMetadata(): FrameMetadata? {
        val currentFrame = currentFrame
        if (currentFrame != null) return currentFrame
        inputStream.transformer = null
        val firstByte = inputStream.readByte()
//        val fin = firstByte and 0x80.toByte() != 0.toByte()
        val opcode = firstByte and 0x0F.toByte()
        check(opcode == 2.toByte() || opcode == 8.toByte()) { "Unsupported websocket opcode for mqtt, opcode: $opcode" }
        if (opcode == 8.toByte()) { // Connection closed
            return null
        }
        var maskedByteLength = inputStream.readByte()
        val masked = maskedByteLength and 0x80.toByte() != 0.toByte()
        var websocketPayloadLength: Int = (0x7F.toByte() and maskedByteLength).toInt()
        var byteCount = when (websocketPayloadLength) {
            0x7F -> 8
            0x7E -> 2
            else -> 0
        }
        if (byteCount > 0) websocketPayloadLength = 0
        while (--byteCount >= 0) {
            maskedByteLength = inputStream.readByte()
            websocketPayloadLength =
                websocketPayloadLength or ((maskedByteLength and 0xFF.toByte()).toInt() shl 8 * byteCount)
        }
        val frame = FrameMetadata(masked, websocketPayloadLength)
        this.currentFrame = frame
        return frame
    }

    suspend fun readPacket(): ControlPacket? {
        try {
            val metadata = readFrameMetadata() ?: return null
            val byte1 = inputStream.readUnsignedByte()
            val remainingLength = inputStream.readVariableByteInteger()
            val bytesRead = UByte.SIZE_BYTES.toUShort() + variableByteSize(remainingLength)
            var extraBytesNeededToFinishControlPacket =
                (metadata.payloadLength.toLong() - bytesRead.toLong()) - remainingLength.toLong()
            return when {
                extraBytesNeededToFinishControlPacket > 0 -> {
                    val buffer = allocateNewBuffer(remainingLength)
                    buffer.write(inputStream.readByteArray(metadata.payloadLength.toLong()))
                    while (extraBytesNeededToFinishControlPacket > 0) {
                        val frameMetadata = readFrameMetadata()
                        currentFrame = frameMetadata
                        if (frameMetadata == null) return null
                        val bytesToRead =
                            min(frameMetadata.payloadLength.toLong(), extraBytesNeededToFinishControlPacket)
                        buffer.write(inputStream.readByteArray(bytesToRead))
                        if (frameMetadata.payloadLength.toLong() == extraBytesNeededToFinishControlPacket) {
                            currentFrame = null
                        }
                        extraBytesNeededToFinishControlPacket -= bytesToRead

                    }
                    controlPacketFactory.from(buffer, byte1, remainingLength)
                }
                extraBytesNeededToFinishControlPacket == 0L -> {
                    val packet = inputStream.readTyped(remainingLength.toLong()) {
                        controlPacketFactory.from(it, byte1, remainingLength)
                    }
                    currentFrame = null
                    packet
                }
                else -> {
                    inputStream.readTyped(remainingLength.toLong()) {
                        controlPacketFactory.from(it, byte1, remainingLength)
                    }
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            // ignore
        }
        return null
    }
}