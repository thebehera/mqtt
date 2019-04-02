package mqtt.client

import kotlinx.io.errors.IOException

open class ConnectionFailureException(msg: String) : IOException(msg)
class FailedToWriteConnectionRequest(e: Exception) : ConnectionFailureException(e.message ?: "")
class FailedToReadConnectionAck(e: Exception) : ConnectionFailureException(e.message ?: "")
class ConnectionTimeout(msg: String) : ConnectionFailureException(msg)
