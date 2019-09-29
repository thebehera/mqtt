package mqtt.client.service.ipc

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SuspendOnIncomingMessageHandler<T> {
    private val observers = ArrayList<Continuation<T>>()

    fun queue(observer: Continuation<T>) {
        observers += observer
    }

    fun notify(obj: T) {
        val continuations = observers.subList(0, observers.size)
        continuations.forEach {
            it.resume(obj)
        }
        continuations.clear()
        // Check if any were added after we notified the continations
        if (observers.isNotEmpty()) {
            notify(obj)
        }
    }

    fun exception(error: Throwable) {
        val continuations = observers.subList(0, observers.size)
        continuations.forEach {
            it.resumeWithException(error)
        }
        continuations.clear()
        // Check if any were added after we notified the continations
        if (observers.isNotEmpty()) {
            exception(error)
        }
    }
}