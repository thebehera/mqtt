package com.ditchoom.mqtt

import android.app.Service
import android.content.Intent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import mqtt.client.ControlPacketWrapper
import mqtt.wire4.control.packet.Reserved

class MqttService : Service() {
    private val mainScope = MainScope()

    val binder = object : IRemoteMqttService.Stub() {
        override fun addServer() {

        }

        override fun removeServer() {

        }

        override fun connect(connackCallback: ControlPacketCallback) {

        }


    }

    override fun onBind(intent: Intent) = binder

    override fun onCreate() {
        super.onCreate()
        ControlPacketWrapper().also { it.packet = Reserved }

    }

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }
}
