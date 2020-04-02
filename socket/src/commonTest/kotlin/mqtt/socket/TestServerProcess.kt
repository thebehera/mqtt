package mqtt.socket

import kotlinx.coroutines.delay
import mqtt.buffer.BufferMemoryLimit
import mqtt.buffer.allocateNewBuffer
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

class TestServerProcess : ServerProcessAbs() {
    public lateinit var name: String
    public lateinit var clientResponse: String
    @ExperimentalTime
    private val readTimeout = 100.milliseconds
    @ExperimentalTime
    private val writeTimeout = readTimeout

    val limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000u
    }

    @ExperimentalTime
    override suspend fun serverSideProcess() {
        val rbuffer = allocateNewBuffer(100.toUInt(), limits)
        val wbuffer = allocateNewBuffer(100.toUInt(), limits)

        assertTrue(socket.isOpen(), "Client socket is not open")

        var num : Int = socket.read(rbuffer, readTimeout)

        var str: String = rbuffer.readMqttUtf8StringNotValidated().toString()

        assertEquals(clientResponse, str.substring(0, clientResponse.length), "Received message is not correct.")

        wbuffer.writeUtf8String(str + ":" + name)

        num = socket.write(wbuffer, writeTimeout)

    }


}