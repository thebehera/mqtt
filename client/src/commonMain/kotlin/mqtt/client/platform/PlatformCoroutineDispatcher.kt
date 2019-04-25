package mqtt.client.platform

import kotlinx.coroutines.CoroutineDispatcher

expect object PlatformCoroutineDispatcher {
    val dispatcher: CoroutineDispatcher
}