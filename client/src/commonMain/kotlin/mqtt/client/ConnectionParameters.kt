package mqtt.client

import mqtt.wire.control.packet.IConnectionRequest

data class ConnectionParameters(
        val hostname: String,
        val port: Int,
        val secure: Boolean,
        val connectionRequest: IConnectionRequest,
        val acceptAllCertificates: Boolean = false,
        val connectionTimeoutMilliseconds: Long = 10_000,
        val maxNumberOfRetries: Int = Int.MAX_VALUE)
