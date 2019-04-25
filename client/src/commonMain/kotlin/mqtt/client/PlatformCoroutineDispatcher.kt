package mqtt.client

import kotlinx.coroutines.CoroutineDispatcher

expect object PlatformCoroutineDispatcher {
    val dispatcher: CoroutineDispatcher
}