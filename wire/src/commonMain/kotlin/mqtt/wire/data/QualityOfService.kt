package mqtt.wire.data

import mqtt.wire.MalformedPacketException

enum class QualityOfService(val integerValue: Byte) {
    AT_MOST_ONCE(0),
    AT_LEAST_ONCE(1),
    EXACTLY_ONCE(2);

    fun toBitInformation(): Pair<Boolean, Boolean> {
        return when (this) {
            AT_MOST_ONCE -> Pair(false, second = false) // 00
            AT_LEAST_ONCE -> Pair(first = false, second = true) // 01
            EXACTLY_ONCE -> Pair(first = true, second = false) // 11
        }
    }

    fun isGreaterThan(otherQos: QualityOfService) = integerValue > otherQos.integerValue
    companion object {
        fun fromBooleans(bit2: Boolean, bit1: Boolean) :QualityOfService {
            return if (bit2 && !bit1) {
                EXACTLY_ONCE
            } else if (!bit2 && bit1) {
                AT_LEAST_ONCE
            } else if (!bit2 && !bit1) {
                AT_MOST_ONCE
            } else {
                throw MalformedPacketException("Invalid flags received, 0x03. Double check QOS is not set to 0x03")
            }
        }
    }
}
