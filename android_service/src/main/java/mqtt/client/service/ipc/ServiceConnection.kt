package mqtt.client.service.ipc

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import mqtt.connection.IMqttConfiguration

class ClientToServiceConnection(val serviceClass: Class<out Service>) : ServiceConnection {

    /** Messenger for communicating with the service. Null if not bound  */
    private var serviceMessenger: Messenger? = null
    private val incomingMessenger: Messenger = Messenger(MessageCallbackHandler {
        if (newConnectionManager.onMessage(it)) {
            return@MessageCallbackHandler
        }
    })
    private val bindManager by lazy { ClientServiceBindManager() }
    private val newConnectionManager by lazy { ClientServiceNewConnectionManager(bindManager, incomingMessenger) }

    fun bind(context: Context) {
        if (isBound()) {
            Log.w("[MQTT][S VM]", "Already bound")
            return
        }
        context.bindService(Intent(context, serviceClass), this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName, serviceBinder: IBinder) {
        val serviceMessenger = Messenger(serviceBinder)
        this.serviceMessenger = serviceMessenger
        registerClientWithService(serviceMessenger)
        bindManager.onServiceConnected(serviceMessenger)
    }

    suspend fun createNewConnection(config: IMqttConfiguration) = newConnectionManager.createConnection(config)

    private fun registerClientWithService(messenger: Messenger) {
        try {
            val message = Message.obtain(null, REGISTER_CLIENT)
            message.replyTo = incomingMessenger
            messenger.send(message)
        } catch (e: RemoteException) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
    }

    fun isBound(): Boolean {
        val serviceMessenger = serviceMessenger
        return serviceMessenger != null && serviceMessenger.binder.isBinderAlive
    }

    override fun onServiceDisconnected(name: ComponentName) {
        serviceMessenger = null
    }

    fun unbind(context: Context) {
        unregisterClientWithService(serviceMessenger)
        context.unbindService(this)
    }

    private fun unregisterClientWithService(messenger: Messenger?) {
        messenger ?: return
        try {
            val message = Message.obtain(null, UNREGISTER_CLIENT)
            message.replyTo = incomingMessenger
            messenger.send(message)
        } catch (e: RemoteException) {
            // There is nothing special we need to do if the service
            // has crashed.
        }
    }

    interface Callback {
        fun onServiceConnected() {}
        fun onServiceSentMessage(msg: Message) {}
        fun onServiceDisconnected() {}
    }
}

class BoundClientsObserver(val callback: (msg: Message) -> Unit) {
    private val registeredClients = HashSet<Messenger>()
    private val incomingHandler = MessageCallbackHandler {
        when (it.what) {
            REGISTER_CLIENT -> registeredClients.add(it.replyTo)
            UNREGISTER_CLIENT -> registeredClients.remove(it.replyTo)
            else -> callback(it)
        }
    }
    val binder = Messenger(incomingHandler).binder!!
}

private const val REGISTER_CLIENT = Int.MIN_VALUE
private const val UNREGISTER_CLIENT = Int.MIN_VALUE + 1

