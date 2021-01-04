package mqtt.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.promise
import mqtt.buffer.*
import mqtt.connection.RemoteHost
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire.control.packet.ControlPacketFactory
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.ARRAYBUFFER
import org.w3c.dom.BinaryType
import org.w3c.dom.WebSocket
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.experimental.and
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class BrowserWebsocketController(
    val scope: CoroutineScope,
    val pool: BufferPool,
    val remoteHost: RemoteHost
) : ISocketController {

    private val websocket: WebSocket =
        WebSocket("ws://${remoteHost.name}:${remoteHost.port}${remoteHost.websocket!!.endpoint}", "mqtt")
    var isConnected = false
    override var lastMessageReceived: TimeMark? = null

    val reader = SuspendableReader(scope, remoteHost.request.controlPacketFactory, websocket)

    init {
        websocket.binaryType = BinaryType.ARRAYBUFFER
        websocket.onclose = {
            isConnected = false
            Unit
        }
        websocket.onerror = {
            isConnected = false
            println("websocket onerror $it")
            Unit
        }
    }

    suspend fun connect() {
        suspendCoroutine<Unit> { continunation ->
            websocket.onopen = { event ->
                isConnected = true
                continunation.resume(Unit)
            }
            websocket.onerror = { event ->
                println("error ${event.type}")
            }
        }
    }

    override suspend fun write(controlPacket: ControlPacket) {
        write(listOf(controlPacket))
    }

    override suspend fun write(controlPackets: Collection<ControlPacket>) {
        val packetSize = controlPackets.fold(0u) { acc, controlPacket -> acc + controlPacket.packetSize() }
        pool.borrow(packetSize) { buffer ->
            val buffer = buffer as JsBuffer
            controlPackets.forEach { packet -> packet.serialize(buffer) }
            buffer.resetForRead()
            val arrayBuffer = buffer.buffer.buffer.slice(buffer.position().toInt(), buffer.limit().toInt())
//            println("writing $controlPackets ${arrayBuffer.byteLength}")
            websocket.send(arrayBuffer)
        }
    }

    override suspend fun read() = flow {
        try {
            while (scope.isActive && isConnected) {
                val packet = reader.readControlPacket()
                lastMessageReceived = reader.lastMessageReceived
                emit(packet)
            }
        } catch (e: ClosedReceiveChannelException) {
            // ignore
        }
    }

    override suspend fun close() {
        reader.incomingChannel.close()
        websocket.close()
    }

    class SuspendableReader(scope: CoroutineScope, val factory: ControlPacketFactory, webSocket: WebSocket) {
        internal val incomingChannel = Channel<JsBuffer>()
        private var currentBuffer: JsBuffer? = null
        var lastMessageReceived: TimeMark? = null

        init {
            webSocket.onmessage = {
                lastMessageReceived = TimeSource.Monotonic.markNow()
                val arrayBuffer = it.data as ArrayBuffer
                val array = Uint8Array(arrayBuffer)
                val buffer = JsBuffer(array)
                buffer.setLimit(array.length)
                buffer.setPosition(0)
                scope.promise {
                    println("sending")
                    incomingChannel.send(buffer)
                    println("sent")
                }
                Unit
            }
        }

        suspend fun readControlPacket(): ControlPacket {
            val byte1 = readUnsignedByte()
            val remainingLength = readVariableByteInteger()
            val readBuffer = nextBuffer(remainingLength.toLong())
            return factory.from(readBuffer, byte1, remainingLength)
        }

        private suspend fun readUnsignedByte() = nextBuffer(UByte.SIZE_BYTES.toLong()).readUnsignedByte()
        private suspend fun readByte() = nextBuffer(UByte.SIZE_BYTES.toLong()).readByte()
        private suspend fun readVariableByteInteger(): UInt {
            var digit: Byte
            var value = 0L
            var multiplier = 1L
            var count = 0
            try {
                do {
                    digit = readByte()
                    count++
                    value += (digit and 0x7F).toLong() * multiplier
                    multiplier *= 128
                } while ((digit and 0x80.toByte()).toInt() != 0)
            } catch (e: Exception) {
                throw MalformedInvalidVariableByteInteger(value.toUInt())
            }
            if (value < 0 || value > VARIABLE_BYTE_INT_MAX.toLong()) {
                throw MalformedInvalidVariableByteInteger(value.toUInt())
            }
            return value.toUInt()
        }

        private suspend fun nextBuffer(size: Long): ReadBuffer {
            check(size > 0)
            val buffer = currentBuffer
            val validBuffer = buffer != null && buffer.hasRemaining()
            val current = if (validBuffer) buffer!! else incomingChannel.receive().also { currentBuffer = it }
            if (current.remaining().toLong() >= size) {
                return current
            }
            var moreBytesToRead = size - current.remaining().toLong()
            val buffers = mutableListOf(current)
            while (moreBytesToRead <= 0) {
                val newBuffer = incomingChannel.receive().also { currentBuffer = it }
                moreBytesToRead -= newBuffer.limit().toLong()
                buffers += newBuffer
            }
            return buffers.toComposableBuffer()
        }
    }
}