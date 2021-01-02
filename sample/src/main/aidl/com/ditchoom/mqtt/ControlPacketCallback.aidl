// ControlPacketCallback.aidl
package com.ditchoom.mqtt;

import mqtt.client.ControlPacketWrapper;
interface ControlPacketCallback {
    void onResponse(out ControlPacketWrapper controlPacketWrapper);
}
