package mqtt.client.service.ipc

import android.os.Handler
import android.os.Message

class MessageCallbackHandler(private val callback: (msg: Message) -> Unit) : Handler() {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        callback(msg)
    }
}

class HandlerFlow(private val callback: (msg: Message) -> Boolean) : Handler.Callback {
    override fun handleMessage(msg: Message) = callback(msg)
}

//fun <T> flowable(callback: Handler.Callback) = callbackFlow<T> {
//    val s = HandlerFlow {
//        if (it is T) {
//
//        }
//    }
//}
class MessageToFlow(private val callback: (msg: Message) -> Boolean) : Handler.Callback {
    override fun handleMessage(msg: Message): Boolean {
        return callback(msg)
    }
}