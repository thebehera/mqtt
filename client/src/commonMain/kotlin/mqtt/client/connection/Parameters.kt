package mqtt.client.connection

import mqtt.wire.control.packet.IConnectionRequest

data class ConnectionParameters(
        val hostname: String,
        val port: Int,
        val secure: Boolean,
        val connectionRequest: IConnectionRequest,
        val acceptAllCertificates: Boolean = false,
        val connectionTimeoutMilliseconds: Long = 10_000,
        val logConnectionAttempt: Boolean = false,
        val logOutgoingPublishOrSubscribe: Boolean = false,
        val logOutgoingControlPackets: Boolean = false,
        val logIncomingControlPackets: Boolean = false,
        val logIncomingPublish: Boolean = false,
        val useWebsockets: Boolean = false,
        val maxNumberOfRetries: Int = Int.MAX_VALUE)
