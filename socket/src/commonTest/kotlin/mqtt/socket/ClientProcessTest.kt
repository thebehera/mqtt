package mqtt.socket

import kotlinx.coroutines.delay
import mqtt.buffer.BufferPool
import mqtt.buffer.PlatformBuffer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

enum class ClientAction {USHORT, CONNECT_DISCONNECT, MQTTSTRING}

class ClientProcessTest (val action: ClientAction, val conType: ConnectionType = ConnectionType.ASYNC):
                            TCPClientProcess (conType){
    @ExperimentalTime
    private val timeout = 100.milliseconds

    public var sendMsg: String = ""
    public var receiveMsg: String = ""

    @ExperimentalTime
    override suspend fun clientSideProcess() {
        when(action) {
            ClientAction.CONNECT_DISCONNECT -> connectDisconnectProcess()
            ClientAction.MQTTSTRING -> mqttStringProcess()
            ClientAction.USHORT -> uShortProcess()
        }
    }

    @ExperimentalTime
    suspend fun uShortProcess() {
        val expectedClientToServer = 4.toUShort()
        val expectedServerToClient = UInt.MAX_VALUE
        val bufferPool = BufferPool()
        var writeBuffer: PlatformBuffer? = null

        assertTrue(isOpen(), "socket should be in open state, but it is not.")
        bufferPool.borrow { writeBuffer = it }
        assertNotNull(writeBuffer, "Failed to get buffer from the pool")
        writeBuffer?.write(expectedClientToServer)

        assertEquals(write(writeBuffer!!, timeout), 2, "Sent message is not correct")
        val (value, size) = read(timeout, {readBuffer, _-> readBuffer.readUnsignedInt()})
        assertEquals(value, expectedServerToClient, "Received message is not correct")
        assertEquals(size, 4, "Received # of bytes is not correct.")
    }

    suspend fun connectDisconnectProcess() {

    }

    @ExperimentalTime
    suspend fun mqttStringProcess() {
        val bufferPool = BufferPool()
        var writeBuffer: PlatformBuffer? = null

        assertTrue(isOpen(), "socket should be in open state, but it is not.")
        bufferPool.borrow { writeBuffer = it }
        assertNotNull(writeBuffer, "Failed to get buffer from the pool")
        writeBuffer?.writeMqttUtf8String(sendMsg)


        assertEquals(sendMsg.length+2, write(writeBuffer!!, timeout), "Sent message is not correct")
        val (value, size) = read(timeout, {readBuffer: PlatformBuffer, _ -> readBuffer.readMqttUtf8StringNotValidated()})
        assertEquals(receiveMsg, value.toString(), "Received value does not match expected")
        assertEquals(value.toString().length + 2, size, "Received data size is not correct")
    }
}