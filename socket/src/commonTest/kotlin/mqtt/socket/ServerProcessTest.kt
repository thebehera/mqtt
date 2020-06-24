@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket

import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.allocateNewBuffer
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

enum class ServerAction {USHORT, CONNECT_DISCONNECT, MQTTSTRING}

class ServerProcessTest (val action: ServerAction) : TCPServerProcess() {
    @ExperimentalTime
    private val timeout = 10000.milliseconds
    var name: String = ""
    var clientResponse: String = ""

    @ExperimentalUnsignedTypes
    val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    @ExperimentalTime
    private suspend fun connectDisProcess() {
        assertTrue(socket.isOpen(), "socket to client is not open")
        socket.read(timeout) { _, _ -> }
    }

    @ExperimentalTime
    private suspend fun uShortProcess() {
        val serverWriteBuffer = allocateNewBuffer(10.toUInt(), limits)
        val recvData: UShort = 4.toUShort()
        val sendData: UInt = UInt.MAX_VALUE

        assertTrue(socket.isOpen(), "socket to client is not open")
        val dataRead = socket.read(timeout) { buffer, bytesRead ->
            buffer.readUnsignedShort()
        }
        assertEquals(2, dataRead.bytesRead, "server received invalid number of data")
        assertEquals(recvData, dataRead.result, "server received invalid data")
        serverWriteBuffer.write(sendData)
        assertEquals(4, socket.write(serverWriteBuffer, timeout), "server send data size not correct")
        socket.close()
    }

    @ExperimentalTime
    @ExperimentalUnsignedTypes
    private suspend fun mqttStringProcess() {
        val wbuffer = allocateNewBuffer(100.toUInt(), limits)

        assertTrue(socket.isOpen(), "Client socket is not open")

        val socketResponse = socket.read(timeout) { buffer, bytesRead ->
            buffer.readMqttUtf8StringNotValidated().toString()
        }
        val str = socketResponse.result
        assertEquals(socketResponse.bytesRead, str.length + 2, "message read length not correct")
        assertEquals(clientResponse, str.substring(0, clientResponse.length), "Received message is not correct.")

        wbuffer.writeMqttUtf8String(str + ":" + name)

        assertEquals(socket.write(wbuffer, timeout), name.length + str.length + 3, "write message length not correct")
    }

    override suspend fun newInstance(): ServerProcess {
        val newProcess = ServerProcessTest(action)
        newProcess.name = this.name
        newProcess.clientResponse = this.clientResponse

        return newProcess
    }

    @ExperimentalTime
    override suspend fun serverSideProcess() {
        when (action) {
            ServerAction.CONNECT_DISCONNECT -> connectDisProcess()
            ServerAction.MQTTSTRING -> mqttStringProcess()
            ServerAction.USHORT -> uShortProcess()
        }
    }
}