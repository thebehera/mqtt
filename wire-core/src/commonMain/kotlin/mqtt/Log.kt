package mqtt

interface Log {
    fun warning(tag: String, msg: String, e: Throwable? = null)
}