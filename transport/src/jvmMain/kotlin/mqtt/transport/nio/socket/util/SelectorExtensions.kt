package mqtt.transport.nio.socket.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.Selector
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
suspend fun Selector.aSelect(timeout: Duration): Int {
    val selector = this
    return withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<Int> {
            try {
                it.resume(select(timeout.toLongMilliseconds()))
            } catch (e: Throwable) {
                if (e is AsynchronousCloseException && it.isCancelled) {
                    selector.close()
                    return@suspendCancellableCoroutine
                }
                it.resumeWithException(e)
            }
        }
    }
}

suspend fun Selector.aClose() = suspendCancellableCoroutine<Unit> {
    try {
        it.resume(close())
    } catch (e: CancellationException) {
        // ignore
    } catch (e: Throwable) {
        it.resumeWithException(e)
    }

}