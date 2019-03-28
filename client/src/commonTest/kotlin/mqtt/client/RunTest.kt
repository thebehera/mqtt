package mqtt.client

import kotlinx.coroutines.CoroutineScope

expect fun <T> block(body: suspend CoroutineScope.() -> T)


