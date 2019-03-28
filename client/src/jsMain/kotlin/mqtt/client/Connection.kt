//@file:Suppress("EXPERIMENTAL_API_USAGE")
//
//package mqtt.client
//
//import kotlinx.coroutines.CoroutineDispatcher
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.io.ByteReadChannel
//import kotlinx.coroutines.io.ByteWriteChannel
//
//actual class Connection actual constructor(override val parameters: ConnectionParameters) : AbstractConnection() {
//
//
//    override lateinit var platformSocket: PlatformSocket
//    override val dispatcher: CoroutineDispatcher = Dispatchers.Default
//
//
//    override suspend fun buildSocket() = object : PlatformSocket {
//        override val output: ByteWriteChannel get() = throw UnsupportedOperationException("JS Not supported yet")
//        override val input: ByteReadChannel get() = throw UnsupportedOperationException("JS Not supported yet")
//
//        override fun dispose() {
//            throw UnsupportedOperationException("JS Not supported yet")
//        }
//
//        override suspend fun awaitClosed() {
//            throw UnsupportedOperationException("JS Not supported yet")
//        }
//
//        override val isClosed: Boolean = true
//
//    }
//
//    override fun beforeClosingSocket() {}
//}
//
