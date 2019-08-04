package mqtt.client.service

import android.content.Context
import android.os.Handler
import android.os.Message

internal class IncomingHandler(context: Context) : Handler() {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        println("Got message $msg")
    }
}