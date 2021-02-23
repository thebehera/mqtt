package mqtt.client

import android.os.Parcel
import android.os.Parcelable
import mqtt.wire.control.packet.ControlPacket
import mqtt.wire4.control.packet.ControlPacketV4
import mqtt.wire5.control.packet.ControlPacketV5

class ControlPacketWrapper : Parcelable {
    var packet: ControlPacket? = null

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        val packet = packet ?: return
        val buffer = ParcelableBuffer(parcel)
        parcel.writeByte(packet.mqttVersion)
        packet.serialize(buffer)
    }

    override fun describeContents() = 0

    fun readFromParcel(reply: Parcel) {
        packet = toControlPacket(reply)
    }

    companion object CREATOR : Parcelable.Creator<ControlPacketWrapper> {

        override fun createFromParcel(parcel: Parcel) =
            ControlPacketWrapper().also { it.packet = toControlPacket(parcel) }

        override fun newArray(size: Int) = arrayOfNulls<ControlPacketWrapper?>(size)

        private fun toControlPacket(parcel: Parcel): ControlPacket {
            val mqttVersion = parcel.readByte()
            val platformBuffer = ParcelableBuffer(parcel)
            return when (mqttVersion) {
                4.toByte() -> ControlPacketV4.from(platformBuffer)
                5.toByte() -> ControlPacketV5.from(platformBuffer)
                else -> throw UnsupportedOperationException("Unsupported mqtt version $mqttVersion")
            }
        }
    }


}