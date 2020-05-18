package mqtt.socket

import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.allocateNewBuffer
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

enum class ServerAction {USHORT, CONNECT_DISCONNECT, MQTTSTRING}

class ServerProcessTest (val action: ServerAction) : TCPServerProcess() {
    @ExperimentalTime
    private val timeout = 10000.milliseconds
    public lateinit var name: String
    public lateinit var clientResponse: String

    @ExperimentalUnsignedTypes
    val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    private suspend fun ConnectProcess () {

    }

    @ExperimentalTime
    private suspend fun ConnectDisProcess () {
        val serverReadBuffer = allocateNewBuffer(10.toUInt(), limits)

        try {
            assertTrue(socket.isOpen(), "socket to client is not open")
            socket.read(serverReadBuffer, timeout)
        } catch (e: Exception) {
            assertEquals("remote returned -1 bytes", e.message, "Unknown exception recevied")
            socket.close()
        }
    }

    @ExperimentalTime
    private suspend fun UShortProcess() {
        val serverReadBuffer = allocateNewBuffer(10.toUInt(), limits)
        val serverWriteBuffer = allocateNewBuffer(10.toUInt(), limits)
        val recvData: UShort = 4.toUShort()
        val sendData: UInt = UInt.MAX_VALUE

        try {
            assertTrue(socket.isOpen(), "socket to client is not open")
            assertEquals(2, socket.read(serverReadBuffer, timeout), "server received invalid data")
            assertEquals(recvData, serverReadBuffer.readUnsignedShort(), "server received invalid data")
            serverWriteBuffer.write(sendData)
            assertEquals(4, socket.write(serverWriteBuffer, timeout), "server send data size not correct")
            socket.close()
        } catch (e: Exception) {
            println("ServerProcessUshort.exception: ${e.message}, $e")
            assertEquals("Failure...", e.message)
        }
    }

    @ExperimentalTime
    @ExperimentalUnsignedTypes
    private suspend fun MqttStringProcess() {
        val rbuffer = allocateNewBuffer(100.toUInt(), limits)
        val wbuffer = allocateNewBuffer(100.toUInt(), limits)

        try {
            assertTrue(socket.isOpen(), "Client socket is not open")

            assertEquals(socket.read(rbuffer, timeout), clientResponse.length + 3, "message read length not correct")

            var str: String = rbuffer.readMqttUtf8StringNotValidated().toString()
            assertEquals(clientResponse, str.substring(0, clientResponse.length), "Received message is not correct.")

            wbuffer.writeUtf8String(str + ":" + name)

            assertEquals(socket.write(wbuffer, timeout), name.length + str.length + 3, "write message length not correct")
        } catch (e: Exception) {
            println("TestServerProcess.serverSideProcess.exception:")
        }
    }

    @ExperimentalTime
    override suspend fun serverSideProcess() {
        when (action) {
            ServerAction.CONNECT_DISCONNECT -> ConnectDisProcess()
            ServerAction.MQTTSTRING -> MqttStringProcess()
            ServerAction.USHORT -> UShortProcess()
        }
    }
}