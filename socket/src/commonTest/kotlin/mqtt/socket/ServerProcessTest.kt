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
        assertTrue(socket.read(timeout) { _, _ -> }.bytesRead > 0)
        socket.close()
    }

    @ExperimentalTime
    private suspend fun uShortProcess() {
        val serverWriteBuffer = allocateNewBuffer(10.toUInt(), limits)
        val recvData: UShort = 4.toUShort()
        val sendData: UInt = UInt.MAX_VALUE

        try {
            assertTrue(socket.isOpen(), "socket to client is not open")
            val socketDataRead = socket.read(timeout) { serverReadBuffer, _ ->
                serverReadBuffer.readUnsignedShort()
            }
            assertEquals(recvData, socketDataRead.result, "server received invalid data")
            assertEquals(2, socketDataRead.bytesRead, "server received invalid data")
            serverWriteBuffer.write(sendData)
            assertEquals(4, socket.write(serverWriteBuffer, timeout), "server send data size not correct")
            socket.close()
        } catch (e: Exception) {
            println("uShortProcess.exception: ${e.message}, $e")
            assertEquals("Failure...", e.message)
        }
    }

    @ExperimentalTime
    @ExperimentalUnsignedTypes
    private suspend fun mqttStringProcess() {
        val wbuffer = allocateNewBuffer(100.toUInt(), limits)

        try {
            assertTrue(socket.isOpen(), "Client socket is not open")

            val (str, ret) = socket.read(timeout) { rbuffer, _ ->
                rbuffer.readMqttUtf8StringNotValidated().toString()
            }
            assertEquals(ret, str.length + 2, "message read length not correct")
            assertEquals(clientResponse, str.substring(0, clientResponse.length), "Received message is not correct.")

            wbuffer.writeMqttUtf8String(str + ":" + name)

            assertEquals(
                socket.write(wbuffer, timeout),
                name.length + str.length + 3,
                "write message length not correct"
            )
        } catch (e: Exception) {
            println("mqttStringProcess.exception: $e, ${e.message}")
        }
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