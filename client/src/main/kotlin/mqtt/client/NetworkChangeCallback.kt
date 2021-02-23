package mqtt.client

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.launch

class NetworkChangeCallback(context: Context): ConnectivityManager.NetworkCallback() {
    private val context = context.applicationContext

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        println("reset reconnect timer cap change")
        resetReconnectingState()
    }

    override fun onAvailable(network: Network) {
        println("reset reconnect timer cap avail")
        resetReconnectingState()
    }

    override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
        if (!blocked) {
            println("reset reconnect timer block change")
            resetReconnectingState()
        }
    }

    private fun resetReconnectingState() {
        networkCallbackScope.launch {
            resetReconnectingTimer(context)
        }
    }
}