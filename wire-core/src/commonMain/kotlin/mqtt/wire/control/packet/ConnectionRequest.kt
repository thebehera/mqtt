@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

interface IConnectionRequest : ControlPacket {
    val keepAliveTimeoutSeconds: UShort

    @ExperimentalTime
    val keepAliveTimeout: Duration
        get() = keepAliveTimeoutSeconds.toInt().toDuration(DurationUnit.SECONDS)
    val clientIdentifier: CharSequence
    val cleanStart: Boolean
    val username: CharSequence?
    val protocolName: CharSequence
    val protocolVersion: Int
    fun copy(): IConnectionRequest
}
