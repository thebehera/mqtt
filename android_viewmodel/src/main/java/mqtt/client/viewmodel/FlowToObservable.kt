package mqtt.client.viewmodel

import androidx.databinding.Observable
import androidx.databinding.ObservableField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ObservableFlow<T : Any>(
    private val flow: Flow<T>,
    override val coroutineContext: CoroutineContext
) : CoroutineScope {
    val observable = ObservableField<T>()

    init {
        launch {
            flow.collect {
                observable.set(it)
            }
        }
    }
}


class NonNullObservableField<T : Any>(value: T, vararg dependencies: Observable) : ObservableField<T>(*dependencies) {
    init {
        set(value)
    }

    override fun get(): T = super.get()!!
    @Suppress("RedundantOverride") // Only allow non-null `value`.
    override fun set(value: T) = super.set(value)
}
