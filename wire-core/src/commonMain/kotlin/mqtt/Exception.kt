package mqtt

import kotlinx.io.errors.IOException

open class ConnectionFailureException(msg: String) : IOException(msg)
class FailedToReadConnectionAck(e: Throwable) : ConnectionFailureException(e.message ?: "")
class ConnectionTimeout(msg: String) : ConnectionFailureException(msg)
