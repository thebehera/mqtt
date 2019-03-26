@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.wire.control.packet

interface IConnectionRequest : ControlPacket {
    val keepAliveTimeoutSeconds: UShort
    fun copy(): IConnectionRequest
}
