package mqtt.android_app

import android.util.Log
import mqtt.ConnectionLogger

class AndroidLogger(logConnection: Boolean = true) : mqtt.Log {
    override val connection = if (logConnection) Connection() else null

    override fun warning(tag: String, msg: String, e: Throwable?) {

    }

    class Connection : ConnectionLogger {
        private val tag = "Mqtt Connection"
        override fun verbose(message: CharSequence, throwable: Throwable?) {
            Log.v(tag, message.toString(), throwable)
        }

        override fun exceptionCausingReconnect(throwable: Throwable) {
            Log.e(tag, "Exception Causing Reconnect", throwable)
        }
    }
}