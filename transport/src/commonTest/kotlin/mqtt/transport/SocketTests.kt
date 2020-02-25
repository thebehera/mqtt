package mqtt.transport

import kotlinx.coroutines.CoroutineScope
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
        val serverSocket = { asyncServerSocket(this, 1, 10.milliseconds, 10.milliseconds, pool) }
        val clientSocket = { asyncClientSocket(this, 10.milliseconds, 10.milliseconds, pool) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(serverLaunched, serverSocket, clientSocket)
    }

    @Test
    fun nio2ConnectDisconnectStress2() = block {
        val serverSocket = { asyncServerSocket(this, 2, 10.milliseconds, 10.milliseconds, pool) }
        val clientSocket = { asyncClientSocket(this, 10.milliseconds, 10.milliseconds, pool) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(serverLaunched, serverSocket, clientSocket)
    }

    @Test
    fun nioNonBlockingConnectDisconnectStress() = block {
        val serverSocket = { asyncServerSocket(this, 1, 10.milliseconds, 10.milliseconds, pool) }
        val clientSocket = { clientSocket(this, false, 10.milliseconds, 10.milliseconds, pool) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(serverLaunched, serverSocket, clientSocket)
    }

    @Test
    fun nioBlockingConnectDisconnectStress() = block {
        val serverSocket = { asyncServerSocket(this, 1, 10.milliseconds, 10.milliseconds, pool) }
        val clientSocket = { clientSocket(this, true, 10.milliseconds, 10.milliseconds, pool) }
        val serverLaunched = launchServer(this, serverSocket)
        stressTest(serverLaunched, serverSocket, clientSocket)
    }

    private suspend fun launchServer(
        scope: CoroutineScope,
        getServerSocket: () -> ServerToClientSocket
    ): ServerLaunched {
        val server = getServerSocket()
        val firstReceiveLock = Mutex(true)
        val mutex = Mutex(true)
        var serverClientSocket: ClientSocket? = null
        val launched = ServerLaunched(firstReceiveLock, server, serverClientSocket, mutex)
        server.bind()
        scope.launch {
            server.listen().collect {
                println("${currentTimestampMs()}      collected $it ${it.localPort()}:${it.remotePort()}")
                serverClientSocket = it
                println("unlock $it")
                firstReceiveLock.unlock()
                println("${currentTimestampMs()}      $it")
                if (++launched.count >= clientCount) {
                    mutex.unlock()
                    return@collect
                }
                it.close()
            }
        }
        return launched
    }

    data class ServerLaunched(
        val firstMessageReceivedLock: Mutex,
        val server: ServerToClientSocket,
        var serverClientSocket: ClientSocket?,
        val mutex: Mutex,
        var count: Int = 0
    )

    private suspend fun stressTest(
        serverL: ServerLaunched?,
        getServerSocket: () -> ServerToClientSocket, getClientSocket: () -> ClientToServerSocket
    ) =
        block {
            val serverLaunched = serverL ?: launchServer(this, getServerSocket)
            repeat(clientCount.toInt()) {
                println("\n${currentTimestampMs()} $it async client")
                val client = getClientSocket()
                serverLaunched.serverClientSocket = client
                client.tag = it.toString()
                val time = measureTime {
                    client.open(port = serverLaunched.server.port()!!)
                }
                serverLaunched.firstMessageReceivedLock.lock()
                println("lock ${serverLaunched.serverClientSocket}")
                val clientPort = client.localPort()

                assertTrue(client.isOpen())
                println("${currentTimestampMs()} $it client($clientPort) opened in $time, closing")
                client.close()
                println("${currentTimestampMs()} $it closed client\n")
                println("server client close ${serverLaunched.serverClientSocket}")
                serverLaunched.serverClientSocket?.close()
            }

            serverLaunched.mutex.lock()
            serverLaunched.server.close()
            println("${currentTimestampMs()}      server close $clientCount clients tested")
            assertEquals(clientCount, serverLaunched.count.toLong())
        }

}
