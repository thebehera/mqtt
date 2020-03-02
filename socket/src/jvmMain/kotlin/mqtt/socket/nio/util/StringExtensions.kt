package mqtt.socket.nio.util

import java.net.InetAddress
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun String?.asInetAddress() = suspendCoroutine<InetAddress> {
    try {
        it.resume(InetAddress.getByName(this))
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }
}