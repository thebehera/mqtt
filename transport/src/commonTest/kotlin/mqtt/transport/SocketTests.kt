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

const val clientCount = 4000L

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@ExperimentalTime
class SocketTests {

    val pool = BufferPool(limits = object : BufferMemoryLimit {
        override fun isTooLargeForMemory(size: UInt) = size > 1_000_000u
    })

    @Test
    fun nio2ConnectDisconnectStress() = block {
        stressTest({
            asyncServerSocket(this, 1, 10.milliseconds, 10.milliseconds, pool)
        }) {
            asyncClientSocket(this, 10.milliseconds, 10.milliseconds, pool)
        }
    }

    @Test
    fun nio2ConnectDisconnectStress2() = block {
        stressTest({
            asyncServerSocket(this, 2, 10.milliseconds, 10.milliseconds, pool)
        }) {
            asyncClientSocket(this, 10.milliseconds, 10.milliseconds, pool)
        }
    }

    @Test
    fun nioNonBlockingConnectDisconnectStress() = block {
        stressTest({
            asyncServerSocket(this, 1, 10.milliseconds, 10.milliseconds, pool)
        }) {
            clientSocket(this, false, 10.milliseconds, 10.milliseconds, pool)
        }
    }

    @Test
    fun nioBlockingConnectDisconnectStress() = block {
        stressTest({
            asyncServerSocket(this, 1, 10.milliseconds, 10.milliseconds, pool)
        }) {
            clientSocket(this, true, 10.milliseconds, 10.milliseconds, pool)
        }
    }

    fun stressTest(getServerSocket: () -> ServerToClientSocket, getClientSocket: () -> ClientToServerSocket) =
        block {
            println()
            var count = 0
            val server = getServerSocket()
            server.bind()
            val firstReceiveLock = Mutex(true)
            val mutex = Mutex(true)
            var serverClientSocket: ClientSocket? = null
            launch {
                server.listen().collect {

                    println("${currentTimestampMs()}      collected $it ${it.localPort()}:${it.remotePort()}")
                    serverClientSocket = it
                    println("unlock $it")
                    firstReceiveLock.unlock()
                    println("${currentTimestampMs()}      $it")
                    if (++count >= clientCount) {
                        mutex.unlock()
                        return@collect
                    }
                    it.close()
                }
            }
            repeat(clientCount.toInt()) {
                println("\n${currentTimestampMs()} $it async client")
                val client = getClientSocket()
                client.tag = it.toString()
                val time = measureTime {
                    client.open(port = server.port()!!)
                }
                firstReceiveLock.lock()
                println("lock $serverClientSocket")
                val clientPort = client.localPort()

                assertTrue(client.isOpen())
                println("${currentTimestampMs()} $it client($clientPort) opened in $time, closing")
                client.close()
                println("${currentTimestampMs()} $it closed client\n")
                println("server client close $serverClientSocket")
                serverClientSocket?.close()
            }

            mutex.lock()
            server.close()
            println("${currentTimestampMs()}      server close $clientCount clients tested")
            assertEquals(clientCount, count.toLong())
        }

}
