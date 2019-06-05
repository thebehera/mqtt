package mqtt.client.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual object PlatformCoroutineDispatcher {
    actual val dispatcher: CoroutineDispatcher = Dispatchers.Default
}