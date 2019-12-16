package mqtt.client

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual object PlatformCoroutineDispatcher {
    actual val dispatcher: CoroutineDispatcher = Dispatchers.Default
}