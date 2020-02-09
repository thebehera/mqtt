package mqtt.transport

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mqtt.time.currentTimestampMs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.milliseconds

const val clientCount = 7_000L

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class SocketTests {
    @Test
    fun nioBlockingConnectDisconnectStress() = block {
        var count = 0
        val server = asyncServerSocket(this, 10.milliseconds, 10.milliseconds)
        server.bind()
        val firstReceiveLock = Mutex(true)
        val mutex = Mutex(true)
        var serverClientSocket: ClientSocket<*>? = null
        launch {
            server.listen().collect {
                //                println("${currentTimestampMs()}      collected ${it.localPort()}:${it.remotePort()}")
                ++count
                firstReceiveLock.unlock()
                serverClientSocket = it
                if (count >= clientCount) {
                    mutex.unlock()
                    return@collect
                }
            }
            server.close()
        }
        repeat(clientCount.toInt()) {
            //            println("\n${currentTimestampMs()} $it async client")
            val client = clientSocket(this, true, 10.milliseconds, 10.milliseconds)
            client.tag = it.toString()
            val time = measureTime {
                client.open(port = server.port()!!)
            }
            firstReceiveLock.lock()
            val clientPort = client.localPort()

            assertTrue(client.isOpen())
            println("${currentTimestampMs()} $it client($clientPort) opened in $time, closing")
            client.close()
//            println("${currentTimestampMs()} $it closed client\n")
            val serverClient = serverClientSocket
            serverClient?.close()
            serverClientSocket = null
        }

        mutex.lock()
        server.close()
        println("${currentTimestampMs()}      server close $clientCount clients tested")
        assertEquals(clientCount, count.toLong())
    }

    @Test
    fun nio2ConnectDisconnectStress() = block {
        var count = 0
        val server = asyncServerSocket(this, 10.milliseconds, 10.milliseconds)
        server.bind()
        val firstReceiveLock = Mutex(true)
        val mutex = Mutex(true)
        var serverClientSocket: ClientSocket<*>? = null
        launch {
            server.listen().collect {
                //                println("${currentTimestampMs()}      collected ${it.localPort()}:${it.remotePort()}")
                ++count
                firstReceiveLock.unlock()
                serverClientSocket = it
                if (count >= clientCount) {
                    mutex.unlock()
                    return@collect
                }
            }
            server.close()
        }
        repeat(clientCount.toInt()) {
//            println("\n${currentTimestampMs()} $it async client")
            val client = asyncClientSocket(this, 10.milliseconds, 10.milliseconds)
            client.tag = it.toString()
            val time = measureTime {
                client.open(port = server.port()!!)
            }
            firstReceiveLock.lock()
            val clientPort = client.localPort()

            assertTrue(client.isOpen())
            println("${currentTimestampMs()} $it client($clientPort) opened in $time, closing")
            client.close()
//            println("${currentTimestampMs()} $it closed client\n")
            val serverClient = serverClientSocket
            serverClient?.close()
            serverClientSocket = null
        }

        mutex.lock()
        server.close()
        println("${currentTimestampMs()}      server close $clientCount clients tested")
        assertEquals(clientCount, count.toLong())
    }
}
