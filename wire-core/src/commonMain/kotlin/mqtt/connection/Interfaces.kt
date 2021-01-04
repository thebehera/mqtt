package mqtt.connection

import mqtt.wire.control.packet.IConnectionRequest
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
data class RemoteHost(
    val name: String,
    val port: Int,
    override val request: IConnectionRequest,
    val security: SecurityParameters = SecurityParameters(false, false),
    val websocket: WebsocketParameters? = null,
    val connectionTimeout: Duration = 1.seconds,
) : IConnectionOptions {
    override val servers =
        hashSetOf(Server(name, port, connectionTimeout, security, websocket))
}

@ExperimentalTime
data class ConnectionOptions(
    override val request: IConnectionRequest,
    override val servers: Set<IConnectionOptions.IServer>,
) : IConnectionOptions

@ExperimentalTime
data class Server(
    override val name: String,
    override val port: Int,
    override val connectionTimeout: Duration = 1.seconds,
    override val security: SecurityParameters,
    override val websocket: WebsocketParameters?
) : IConnectionOptions.IServer


interface IConnectionOptions {
    interface IServer {
        val name: String
        val port: Int

        @ExperimentalTime
        val connectionTimeout: Duration
        val security: SecurityParameters
        val websocket: WebsocketParameters?
    }

    val request: IConnectionRequest
    val servers: Set<IServer>
}

class WebsocketParameters(val endpoint: String)

class SecurityParameters(
    val isTransportLayerSecurityEnabled: Boolean,
    val acceptAllCertificates: Boolean
)