package mqtt.client.service

import android.os.Handler
import android.os.Message

internal class IncomingHandler(private val context: OnClientMessageReceived) : Handler() {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        val obj = msg.obj ?: return
        context.onMessage(obj)
    }
}