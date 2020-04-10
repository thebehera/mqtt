package mqtt.wire.data

import kotlinx.io.charsets.Charsets
import kotlinx.io.charsets.encodeToByteArray
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.readBytes
import mqtt.Parcelable
import mqtt.Parcelize

@Parcelize
data class ByteArrayWrapper(val byteArray: ByteArray) : Parcelable {
    constructor(text: CharSequence) : this(Charsets.UTF_8.newEncoder().encodeToByteArray(text))
    constructor(packet: ByteReadPacket?) : this(packet?.readBytes() ?: ByteArray(0))
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ByteArrayWrapper

        if (!byteArray.contentEquals(other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int {
        return byteArray.contentHashCode()
    }
}
