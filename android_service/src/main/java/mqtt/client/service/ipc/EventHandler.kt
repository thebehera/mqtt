package mqtt.client.service.ipc

import android.os.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mqtt.connection.IMqttConfiguration
import kotlin.coroutines.CoroutineContext

class EventHandler(
    override val coroutineContext: CoroutineContext
) : CoroutineScope {
    fun handleMessage(msg: Message) {
        val data = msg.data
        val config = data.getParcelable<IMqttConfiguration>("config")
        val obj = msg.obj
        launch {
            if (obj != null) {
                obj.toString()
            }
            if (data != null) {
                data.toString()
            }
            if (config != null) {
                config.toString()
            }
        }
    }

}
