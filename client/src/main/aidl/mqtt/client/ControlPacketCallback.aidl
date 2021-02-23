// ControlPacketCallback.aidl
package mqtt.client;

import mqtt.client.ControlPacketWrapper;

interface ControlPacketCallback {
    void onMessage(inout ControlPacketWrapper controlPacketWrapper);
    void onMessageFd(String fileReference);
}
