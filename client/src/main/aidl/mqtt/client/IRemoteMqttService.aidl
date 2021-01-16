// IRemoteMqttService.aidl
package mqtt.client;

// Declare any non-default types here with import statements
import mqtt.client.ControlPacketCallback;

interface IRemoteMqttService {
    void addServer(long connectionId, byte mqttVersion);
    void removeServer(long connectionId);
    void connect(long connectionId, ControlPacketCallback connackCallback);
}