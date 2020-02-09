package mqtt.transport.nio.socket.util

import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.NetworkChannel
import java.nio.channels.SocketChannel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
suspend fun NetworkChannel.aClose() {
    suspendCoroutine<Unit> { cont ->
        blockingClose()
        cont.resume(Unit)
    }
}


@ExperimentalTime
internal fun NetworkChannel.blockingClose() {
    val text = toString()
    val time = measureTime {
        try {
            if (this is SocketChannel) {
                shutdownInput()
            } else if (this is AsynchronousSocketChannel) {
                shutdownInput()
            }
        } catch (ex: Throwable) {
        }
        try {
            if (this is SocketChannel) {
                shutdownOutput()
            } else if (this is AsynchronousSocketChannel) {
                shutdownOutput()
            }
//            println("${currentTimestampMs()} shutdown output")
        } catch (ex: Throwable) {
        }
        try {
            close()
//            println("${currentTimestampMs()} closed ${!isOpen}")
        } catch (ex: Throwable) {
            // Specification says that it is Ok to call it any time, but reality is different,
            // so we have just to ignore exception
        }
    }
//    if (time > minTimeBeforeLogging) {
//        println("${currentTimestampMs()} took $time to close $text $this")
//    }
}