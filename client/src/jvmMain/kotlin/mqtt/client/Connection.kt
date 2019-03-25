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

actual fun CoroutineScope.openSocket(parameters: ConnectionParameters) = launch {
    val socketBuilder = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
    val address = InetSocketAddress(parameters.hostname, parameters.port)
    val connectionAttemptTime = currentTimestampMs()
    val socket = socketBuilder.connect(address)
    val socketConnectedTime = currentTimestampMs()
    val socketConnectionTimeMs = socketConnectedTime - connectionAttemptTime
    println("Connected socket in $socketConnectionTimeMs ms")
    val output = socket.openWriteChannel(autoFlush = true)
    val writeChannelTime = currentTimestampMs()
    val connectionRequestPacket = parameters.connectionRequest.copy().serialize().readBytes()
    val serializationTime = currentTimestampMs()
    val processTime = serializationTime - writeChannelTime
    println("Socket processing time took $processTime ms")
    output.writeFully(connectionRequestPacket)
    val postWriteTime = currentTimestampMs()
    val socketWriteTime = postWriteTime - serializationTime
    println("OUT [${connectionRequestPacket.size}][$socketWriteTime]: ${parameters.connectionRequest}")


    var lastValidatedMessageBetweenBroker = currentTimestampMs()
    launch {
        val input = socket.openReadChannel()
        val initialControlPacket = input.read()
        if (initialControlPacket is ConnectionAcknowledgment) {
            val postConnackTime = currentTimestampMs()
            println("Connected in ${postConnackTime - postWriteTime} ms")
            runKeepAlive(parameters.connectionRequest, lastValidatedMessageBetweenBroker, parameters.clientToBroker)
        }
        while (isActive) {
            val controlPacket = input.read()
            parameters.brokerToClient.send(controlPacket)
            lastValidatedMessageBetweenBroker = currentTimestampMs()
        }
    }
    for (messageToSend in parameters.clientToBroker) {
        if (isActive) {
            val sendMessage = messageToSend.serialize()
            val size = sendMessage.remaining
            val start = currentTimestampMs()
            output.writePacket(sendMessage)
            val sendTime = currentTimestampMs() - start
            lastValidatedMessageBetweenBroker = currentTimestampMs()
            println("OUT [$size][$sendTime]: $messageToSend")
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
    val clientToBroker = Channel<ControlPacket>()
    val brokerToClient = Channel<ControlPacket>()
    val params = ConnectionParameters("localhost", 1883, ConnectionRequest(header, payload), clientToBroker, brokerToClient)
    while (true) {
        var job: Job? = null
        try {
            job = runBlocking {
                launch {
                    while (isActive) {
                        val packet = select<ControlPacket> { brokerToClient.onReceive { it } }
                        val packetSize = packet.serialize().remaining
                        println("IN  [$packetSize]: $packet")
                    }
                }
                val socketJob = openSocket(params)
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
