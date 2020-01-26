package mqtt.transport.nio2.util

import kotlinx.coroutines.CancellableContinuation
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object AsyncVoidIOHandler :
    CompletionHandler<Void?, CancellableContinuation<Unit>> {
    override fun completed(result: Void?, cont: CancellableContinuation<Unit>) {
        cont.resume(Unit)
    }

    override fun failed(ex: Throwable, cont: CancellableContinuation<Unit>) {
        // just return if already cancelled and got an expected exception for that case
        if (ex is AsynchronousCloseException && cont.isCancelled) return
        cont.resumeWithException(ex)
    }
}

internal object AsyncIOHandlerAny :
    CompletionHandler<Any, CancellableContinuation<Any>> {
    override fun completed(result: Any, cont: CancellableContinuation<Any>) {
        cont.resume(result)
    }

    override fun failed(ex: Throwable, cont: CancellableContinuation<Any>) {
        // just return if already cancelled and got an expected exception for that case
        if (ex is AsynchronousCloseException && cont.isCancelled) return
        cont.resumeWithException(ex)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> asyncIOHandler(): CompletionHandler<T, CancellableContinuation<T>> =
    AsyncIOHandlerAny as CompletionHandler<T, CancellableContinuation<T>>