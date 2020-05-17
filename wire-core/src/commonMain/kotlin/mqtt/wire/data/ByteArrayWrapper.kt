package mqtt.wire.data

import kotlin.reflect.KClass

data class ByteArrayWrapper(val byteArray: ByteArray) {
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

data class GenericType<T : Any>(val obj: T, val kClass: KClass<T>) {

    companion object {
        inline fun <reified T : Any> create(obj: T): GenericType<T> = GenericType(obj, T::class)
    }
}