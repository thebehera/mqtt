// ControlPacketCallback.aidl
package mqtt.client;

import mqtt.client.ControlPacketWrapper;
interface ControlPacketCallback {
    void onResponse(out ControlPacketWrapper controlPacketWrapper);
}
