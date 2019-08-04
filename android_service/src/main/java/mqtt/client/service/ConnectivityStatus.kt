package mqtt.client.service

class ConnectivityStatusManager {


    interface OnConnectivityChangedListener {
        fun onConnectivityChanged(isConnectedOrConnecting: Boolean)
    }
}