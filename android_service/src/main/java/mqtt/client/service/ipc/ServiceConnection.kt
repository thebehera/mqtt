package mqtt.client.service.ipc

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import mqtt.client.service.MqttConnectionsDatabaseDescriptor
import mqtt.client.service.REGISTER_CLIENT
import mqtt.client.service.UNREGISTER_CLIENT
import mqtt.connection.IRemoteHost

class ClientToServiceConnection(
    context: Context, serviceClass: Class<out Service>,
    dbProvider: MqttConnectionsDatabaseDescriptor
) : ServiceConnection {

    /** Messenger for communicating with the service. Null if not bound  */
    private var serviceMessenger: Messenger? = null
    private val incomingMessenger: Messenger = Messenger(MessageCallbackHandler {
        if (newConnectionManager.onMessage(it)) {
            return@MessageCallbackHandler
        }
    })
    private val bindManager by lazy { ClientServiceBindManager() }
    val newConnectionManager = ClientServiceNewConnectionManager(context, bindManager, incomingMessenger)

    init {
        val intent = Intent(context, serviceClass)
        intent.putExtra(MqttConnectionsDatabaseDescriptor.TAG, dbProvider)
        Log.i("RAHUL", "Binding service")
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName, serviceBinder: IBinder) {
        Log.i("RAHUL", "Service Connected")
        val serviceMessenger = Messenger(serviceBinder)
        this.serviceMessenger = serviceMessenger
        registerClientWithService(serviceMessenger)
        bindManager.onServiceConnected(serviceMessenger)
    }

    suspend fun createNewConnection(remoteHost: IRemoteHost, awaitOnConnectionState: Int?) =
        newConnectionManager.createConnection(remoteHost, awaitOnConnectionState)

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

    private fun buildPublishBundle(rowId: Long, tableName: String): Bundle {
        val bundle = Bundle()
        bundle.putLong(rowIdKey, rowId)
        bundle.putString(tableNameKey, tableName)
        return bundle
    }

    suspend fun notifyPublish(rowId: Long, tableName: String) {
        val message = Message.obtain(null, BoundClientToService.QUEUE_INSERTED.position)
        message.replyTo = incomingMessenger
        message.data = buildPublishBundle(rowId, tableName)
        bindManager.awaitServiceBound().send(message)
    }
}
