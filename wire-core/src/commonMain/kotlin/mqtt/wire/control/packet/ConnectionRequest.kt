@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

import mqtt.Parcelable

interface IConnectionRequest : ControlPacket, Parcelable {
    val keepAliveTimeoutSeconds: UShort
    val clientIdentifier: String
    val cleanStart: Boolean
    val username: String?
    val protocolName: String
    val protocolVersion: Int
    fun copy(): IConnectionRequest
}
