package mqtt.client.platform

import kotlinx.coroutines.Dispatchers

actual object PlatformCoroutineDispatcher {
    // Should be a IO dispatcher once it is supported
    actual val dispatcher = Dispatchers.Default

}