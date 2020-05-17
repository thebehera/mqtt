package mqtt.wire.data

import mqtt.IgnoredOnParcel
import mqtt.Parcelable
import mqtt.Parcelize
import kotlin.reflect.KClass

@Parcelize
data class ByteArrayWrapper(val byteArray: ByteArray) : Parcelable {
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

sealed class GenericType<T : Any>(val obj: T, val kClass: KClass<T>) {
    @Parcelize
    data class ParcelableGenericType<T : Any>(private val o: T, @IgnoredOnParcel private val c: KClass<T>) :
        GenericType<T>(o, c), Parcelable {
        val x = obj::class
    }

    data class Generic<T : Any>(private val o: T, private val c: KClass<T>) : GenericType<T>(o, c)

    companion object {
        inline fun <reified T : Any> create(obj: T) = if (obj is Parcelable) {
            ParcelableGenericType(obj, T::class)
        } else {
            Generic(obj, T::class)
        }
    }
}