//package mqtt.client
//
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.asPromise
//import kotlinx.coroutines.async
//import kotlin.js.Promise
//
//actual fun <T> block(body: suspend CoroutineScope.() -> T): dynamic {
//    return GlobalScope.async(block = body).asPromise()
//}
