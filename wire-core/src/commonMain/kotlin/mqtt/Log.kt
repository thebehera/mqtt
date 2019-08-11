package mqtt

interface Log {
    val connection: ConnectionLogger?
    fun warning(tag: String, msg: String, e: Throwable? = null)
}

interface ConnectionLogger {
    fun verbose(message: CharSequence, throwable: Throwable? = null)

    fun exceptionCausingReconnect(throwable: Throwable)
}

class NoOpLog(override val connection: ConnectionLogger? = null) : Log {
    override fun warning(tag: String, msg: String, e: Throwable?) {}
}