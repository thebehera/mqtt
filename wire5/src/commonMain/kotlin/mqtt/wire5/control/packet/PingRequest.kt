package mqtt.wire5.control.packet

import kotlinx.io.core.readBytes
import mqtt.IgnoredOnParcel
import mqtt.Parcelize
import mqtt.wire.control.packet.IPingRequest
import mqtt.wire.control.packet.format.fixed.DirectionOfFlow

/**
 * 3.12 PINGREQ – PING request
 * The PINGREQ packet is sent from a Client to the Server. It can be used to:
 *
 * ·         Indicate to the Server that the Client is alive in the absence of any other MQTT Control Packets being
 * sent from the Client to the Server.
 *
 * ·         Request that the Server responds to confirm that it is alive.
 *
 * ·         Exercise the network to indicate that the Network Connection is active.
 *
 * This packet is used in Keep Alive processing. Refer to section 3.1.2.10 for more details.
 */
@Parcelize
object PingRequest : ControlPacketV5(12, DirectionOfFlow.CLIENT_TO_SERVER), IPingRequest {
    @IgnoredOnParcel
    override val lazyBytes: ByteArray by lazy { serialize().readBytes() }
}
