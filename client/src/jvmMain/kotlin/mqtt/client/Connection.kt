@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.client

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.awaitClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.io.writeFully
import kotlinx.coroutines.selects.select
import kotlinx.io.core.readBytes
import mqtt.time.currentTimestampMs
import mqtt.wire.control.packet.*
import mqtt.wire.data.MqttUtf8String
import mqtt.wire.data.QualityOfService
import java.io.IOException
import java.net.InetSocketAddress

actual fun CoroutineScope.openSocket(hostname: String, port: Int, connectionRequest: ConnectionRequest,
                                     clientToBroker: Channel<ControlPacket>,
                                     brokerToClient: SendChannel<ControlPacket>) = launch {
    val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
            .connect(InetSocketAddress(hostname, port))

    var lastValidatedMessageBetweenBroker = currentTimestampMs()
    launch {
        val output = socket.openWriteChannel(autoFlush = true)
        val connectionRequestPacket = connectionRequest.copy().serialize().readBytes()
        output.writeFully(connectionRequestPacket)

        launch {
            val input = socket.openReadChannel()
            while (isActive) {
                val controlPacket = input.read()
                if (controlPacket is ConnectionAcknowledgment) {
                    runKeepAlive(connectionRequest, lastValidatedMessageBetweenBroker, clientToBroker)
                }
                brokerToClient.send(controlPacket)
                lastValidatedMessageBetweenBroker = currentTimestampMs()
            }
        }
        for (messageToSend in clientToBroker) {
            if (isActive) {
                output.writePacket(messageToSend.serialize())
                println("OUT: $messageToSend")
                lastValidatedMessageBetweenBroker = currentTimestampMs()
            }
        }
    }
    socket.awaitClosed()
}


fun CoroutineScope.runKeepAlive(
        connectionRequest: ConnectionRequest,
        lastValidatedMessageBetweenBroker: Long,
        clientToBroker: SendChannel<ControlPacket>) = launch {
    val keepAliveTimeoutSeconds = connectionRequest.variableHeader.keepAliveSeconds.toLong()
    if (keepAliveTimeoutSeconds > 0) {
        val keepAliveTimeoutMs = keepAliveTimeoutSeconds * 1000
        while (isActive) {
            val timeLeftMs = lastValidatedMessageBetweenBroker - currentTimestampMs()
            if (timeLeftMs > keepAliveTimeoutMs) {
                delay(timeLeftMs)
                continue
            } else {
                clientToBroker.send(PingRequest)
                delay(keepAliveTimeoutMs)
            }
        }
    }
}

suspend fun close(brokerToClient: SendChannel<ControlPacket>) {
    brokerToClient.send(DisconnectNotification())
}

fun main() {
    val header = ConnectionRequest.VariableHeader(
            protocolVersion = 4.toUByte(),
            keepAliveSeconds = 2.toUShort(),
            cleanStart = true,
            willQos = QualityOfService.AT_MOST_ONCE)
    val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("thebehera163224626"))

    while (true) {
        var job: Job? = null
        try {
            job = runBlocking {
                val clientToBroker = Channel<ControlPacket>()
                val brokerToClient = Channel<ControlPacket>()
                launch {
                    while (isActive) {
                        val packet = select<ControlPacket> { brokerToClient.onReceive { it } }
                        println("IN : $packet")
                    }
                }
                val socketJob = openSocket("localhost", 1883, ConnectionRequest(header, payload), clientToBroker, brokerToClient)
                socketJob
            }
        } catch (e: ClosedReceiveChannelException) {
            println("${currentTimestampMs()} Socket closed received from server")
            job?.cancel()
        } catch (e: IOException) {
            println("${currentTimestampMs()} failed to connect to server, trying again immediately")
            job?.cancel()
        } finally {
            job?.cancel()
        }
    }
}
