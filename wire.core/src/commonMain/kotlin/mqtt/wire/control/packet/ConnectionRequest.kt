@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

interface IConnectionRequest : ControlPacket {
    val keepAliveTimeoutSeconds: UShort
    val clientIdentifier: String
    val cleanStart: Boolean
    fun copy(): IConnectionRequest
}
