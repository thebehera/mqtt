package mqtt.client

import mqtt.wire.control.packet.IConnectionRequest

data class ConnectionParameters(
        val hostname: String,
        val port: Int,
        val secure: Boolean,
        val connectionRequest: IConnectionRequest,
        val acceptAllCertificates: Boolean = false,
        val reconnectIfNetworkLost: Boolean = true,
        val connectionTimeoutMilliseconds: Long = 10_000,
        // Ignored if reconnectIfNetworkLost is false
        val maxNumberOfRetries: Int = Int.MAX_VALUE)
