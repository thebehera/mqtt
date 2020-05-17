package mqtt.buffer

import kotlin.reflect.KClass

data class GenericType<T : Any>(val obj: T, val kClass: KClass<T>)