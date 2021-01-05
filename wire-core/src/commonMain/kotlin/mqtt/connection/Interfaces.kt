package mqtt.connection

import mqtt.wire.control.packet.IConnectionRequest
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
data class ConnectionOptions(
    override val name: String,
    override val port: Int,
    override val request: IConnectionRequest,
    override val connectionTimeout: Duration = 1.seconds,
    override val websocketEndpoint: String? = null,
) : IConnectionOptions


interface IConnectionOptions {
    val name: String
    val port: Int

    @ExperimentalTime
    val connectionTimeout: Duration
    val websocketEndpoint: String?
    val request: IConnectionRequest
}