package mqtt.connection

import mqtt.wire.control.packet.IConnectionRequest
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@ExperimentalTime
data class RemoteHost(
    override val name: String,
    override val port: Int,
    override val request: IConnectionRequest,
    override val security: SecurityParameters = SecurityParameters(false, false),
    override val websocket: WebsocketParameters? = null,
    override val connectionTimeoutMs: Milliseconds = 1000,
    override val connectionTimeout: Duration = connectionTimeoutMs.toDuration(DurationUnit.MILLISECONDS),
) : IRemoteHost {
    data class WebsocketParameters(override val endpoint: String = "/") : IRemoteHost.IWebsocketParameters
    data class SecurityParameters(
        override val isTransportLayerSecurityEnabled: Boolean,
        override val acceptAllCertificates: Boolean
    ) : IRemoteHost.ISecurityParameters
}

interface IRemoteHost {
    interface IWebsocketParameters {
        val endpoint: String
    }

    interface ISecurityParameters {
        val isTransportLayerSecurityEnabled: Boolean
        val acceptAllCertificates: Boolean
    }

    val name: String
    val port: Int
    val connectionTimeoutMs: Milliseconds

    @ExperimentalTime
    val connectionTimeout: Duration
    val security: ISecurityParameters
    val websocket: IWebsocketParameters?
    val request: IConnectionRequest

    fun connectionIdentifier() = uniqueIdentifier().hashCode()
    fun uniqueIdentifier(): CharSequence = Companion.uniqueIdentifier(
        request.protocolName, request.protocolVersion,
        request.clientIdentifier, name, port
    )

    companion object {
        fun uniqueIdentifier(
            protocolName: CharSequence,
            protocolVersion: Int,
            clientId: CharSequence,
            name: CharSequence,
            port: Int
        ) =
            listOf(
                protocolName,
                protocolVersion,
                clientId,
                name,
                port
            ).joinToString(".")
    }
}

typealias Milliseconds = Long
