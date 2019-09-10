package mqtt.client.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import mqtt.client.service.ipc.BoundClientToService

class SingleMqttClientServiceConnection : ServiceConnection {
    /** Messenger for communicating with the service. Null if not bound  */
    private var mService: Messenger? = null
    private val handler = Handler()

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
        // This is called when the connection with the service has been
        // established, giving us the object we can use to
        // interact with the service.  We are communicating with the
        // service using a Messenger, so here we get a client-side
        // representation of that from the raw IBinder object.
        mService = Messenger(service)

    }

    fun queuedInserted() {
        mService?.send(Message.obtain(handler, BoundClientToService.QUEUE_INSERTED.ordinal))
    }

    override fun onServiceDisconnected(className: ComponentName) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        mService = null

    }
}
