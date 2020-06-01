@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

interface IConnectionRequest : ControlPacket {
    val keepAliveTimeoutSeconds: UShort
    val clientIdentifier: CharSequence
    val cleanStart: Boolean
    val username: CharSequence?
    val protocolName: CharSequence
    val protocolVersion: Int
    fun copy(): IConnectionRequest
}
