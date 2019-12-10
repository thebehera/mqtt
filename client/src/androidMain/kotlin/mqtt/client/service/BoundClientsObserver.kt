package mqtt.client.service

import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import mqtt.client.service.ipc.MessageCallbackHandler

class BoundClientsObserver(newClientCb: (Messenger) -> Unit, val callback: (msg: Message) -> Unit) {
    private val registeredClients = LinkedHashSet<Messenger>()
    private val incomingHandler = MessageCallbackHandler {
        when (it.what) {
            REGISTER_CLIENT -> {
                registeredClients.add(it.replyTo)
                newClientCb(it.replyTo)
            }
            UNREGISTER_CLIENT -> registeredClients.remove(it.replyTo)
            else -> callback(it)
        }
    }
    val messenger = Messenger(incomingHandler)
    val binder = messenger.binder!!


    fun sendMessageToClients(msg: Message) = LinkedHashSet(registeredClients).forEach {
        try {
            it.send(msg)
        } catch (e: RemoteException) {
            // unregister the client, there is nothing we can do at this point as the other process has crashed
            registeredClients.remove(it)
        }
    }
}