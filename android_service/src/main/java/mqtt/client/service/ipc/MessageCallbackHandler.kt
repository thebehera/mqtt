
package mqtt.client.service.ipc

import android.os.Handler
import android.os.Message

class MessageCallbackHandler(private val callback: (msg: Message) -> Unit) : Handler() {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        callback(msg)
    }
}
