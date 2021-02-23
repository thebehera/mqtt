package mqtt.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import kotlinx.coroutines.*

class NetworkChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // any change fires a reset to the reconnecting timer
        networkCallbackScope.launch {
            println("reset reconnect timer recv")
            val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.allNetworks
            resetReconnectingTimer(context)
        }
    }
}

internal fun registerNetworkListener(context: Context) {
    val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    when {
        VERSION.SDK_INT >= VERSION_CODES.N -> {
            connectivityManager.registerDefaultNetworkCallback(NetworkChangeCallback(context))
        }
        VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP -> {
            val request = NetworkRequest.Builder()
                .addCapability(NET_CAPABILITY_INTERNET).build()
            connectivityManager.registerNetworkCallback(request, NetworkChangeCallback(context))
        }
        else -> {
            context.registerReceiver(NetworkChangeReceiver(), IntentFilter(CONNECTIVITY_ACTION))
        }
    }
}

internal val networkCallbackScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
internal suspend fun CoroutineScope.resetReconnectingTimer(context: Context) {
    val serviceConnection = MqttAppServiceConnection.getMqttServiceConnection(context, this)
    serviceConnection.resetReconnectTimer()
    serviceConnection.pingAsync().await()
    context.unbindService(serviceConnection.serviceConnection)
}