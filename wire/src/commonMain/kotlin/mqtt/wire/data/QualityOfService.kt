package mqtt.wire.data

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
}
