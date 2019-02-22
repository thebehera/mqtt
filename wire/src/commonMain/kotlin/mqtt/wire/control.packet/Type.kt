package mqtt.wire.control.packet

import mqtt.wire.control.packet.DirectionOfFlow.CLIENT_TO_SERVER
import mqtt.wire.control.packet.DirectionOfFlow.FORBIDDEN

/**
 * <table><tr><td><b>Bit</b><td><b>7</b><td><b>6</b><td><b>5</b><td><b>4</b><td><b>3</b><td><b>2</b><td><b>1</b><td><b>0</b><tr><td>byte 1<td>MQTT Control Packet type<td>Flags specific to each MQTT Control Packet type<tr><td>byte 2<td>Remaining Length</table>
 */
@Suppress("UNUSED_PARAMETER")
enum class ControlPacketType(value: Byte, direction: DirectionOfFlow) {
    RESERVED(0, FORBIDDEN),
    /**
     * Connection request
     */
    CONNECT(1, CLIENT_TO_SERVER)

}
